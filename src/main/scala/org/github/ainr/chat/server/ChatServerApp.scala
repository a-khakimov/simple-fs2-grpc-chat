package org.github.ainr.chat.server

import cats.effect.{ExitCode, IO, IOApp}
import fs2.concurrent.Topic
import fs2.grpc.server.ServerOptions
import fs2.grpc.syntax.all._
import io.grpc.ServerServiceDefinition
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import org.github.ainr.chat.{ChatServiceFs2Grpc, StreamData}

import java.util.concurrent.TimeUnit

object ChatServerApp extends IOApp {

  def runServer(service: ServerServiceDefinition): IO[Nothing] = {
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
    _ <- ChatServiceFs2Grpc.bindServiceResource[IO](
      ChatService[IO](topic),
      ServerOptions.default
    ).use(runServer)
  } yield ExitCode.Success
}
