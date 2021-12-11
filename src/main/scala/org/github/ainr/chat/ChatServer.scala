package org.github.ainr.chat

import cats.Applicative
import cats.effect.kernel.Temporal
import cats.effect.std.Console
import cats.effect.{ExitCode, IO, IOApp, Resource}
import fs2.Stream
import fs2.concurrent.Topic
import fs2.grpc.server.ServerOptions
import fs2.grpc.syntax.all._
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.{Metadata, ServerServiceDefinition}

import java.util.concurrent.TimeUnit

class ChatServiceImpl[
  F[_]
  : Applicative
  : Temporal
](
  eventsTopic: Topic[F, org.github.ainr.chat.StreamData]
)(
  implicit
  console: Console[F]
) extends ChatServiceFs2Grpc[F, Metadata] {

  val events: Stream[F, StreamData] =
    eventsTopic
      .subscribe(100)
      .evalTap(data => console.println(s"From topic: $data"))

  override def chatStream(
    request: fs2.Stream[F, StreamData],
    ctx: Metadata
  ): fs2.Stream[F, StreamData] = {
    events.concurrently(
      request
        .evalTap(s => console.println(s"From request: $s"))
        .evalTap(eventsTopic.publish1)
    )
  }
}

object ChatService extends IOApp {

  def chatService(topic: Topic[IO, StreamData]): Resource[IO, ServerServiceDefinition] = {
    ChatServiceFs2Grpc.bindServiceResource[IO](
      new ChatServiceImpl[IO](topic),
      ServerOptions.default
    )
  }

  def runService(service: ServerServiceDefinition): IO[Nothing] = {
    NettyServerBuilder
      .forPort(50053)
      .keepAliveTime(500, TimeUnit.SECONDS)
      .addService(service)
      .resource[IO]
      .evalMap(server => IO(server.start()))
      .useForever
  }

  override def run(args: List[String]): IO[ExitCode] = for {
    topic <- Topic[IO, StreamData]
    _ <- chatService(topic).use(runService)
  } yield ExitCode.Success
}

