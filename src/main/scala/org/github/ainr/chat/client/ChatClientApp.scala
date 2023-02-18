package org.github.ainr.chat.client

import cats.effect.{ExitCode, IO, IOApp, Resource}
import fs2.grpc.syntax.all.fs2GrpcSyntaxManagedChannelBuilder
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.{Channel, Metadata}
import org.github.ainr.chat.ChatServiceFs2Grpc

object ChatClientApp extends IOApp {

  private def buildChatService(channel: Channel): Resource[IO, ChatServiceFs2Grpc[IO, Metadata]] =
    ChatServiceFs2Grpc.stubResource[IO](channel)

  private def resources: Resource[IO, ChatServiceFs2Grpc[IO, Metadata]] =
    NettyChannelBuilder
      .forAddress("127.0.0.1", 50053)
      .usePlaintext()
      .resource[IO]
      .flatMap(buildChatService)

  override def run(args: List[String]): IO[ExitCode] =
    resources.use { chatServiceFs2Grpc =>
      ChatClient(
        args.headOption.getOrElse("Anonymous"),
        InputStream[IO](bufSize = 1024),
        chatServiceFs2Grpc
      ).start
    }.as(ExitCode.Success)
}
