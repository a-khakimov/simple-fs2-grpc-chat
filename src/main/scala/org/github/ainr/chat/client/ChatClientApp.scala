package org.github.ainr.chat.client

import cats.effect.{ExitCode, IO, IOApp, Resource}
import fs2.grpc.client.ClientOptions
import fs2.grpc.syntax.all.fs2GrpcSyntaxManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import org.github.ainr.chat.ChatServiceFs2Grpc

object ChatClientApp extends IOApp {

  def resources: Resource[IO, ChatServiceFs2Grpc[IO, Metadata]] = for {
    channel <- NettyChannelBuilder.forAddress("127.0.0.1", 50053).usePlaintext().resource[IO]
    chatClient <- ChatServiceFs2Grpc.stubResource[IO](channel, ClientOptions.default)
  } yield chatClient

  override def run(args: List[String]): IO[ExitCode] =
    resources.use { chatServiceFs2Grpc =>
      val name = args.headOption.getOrElse("Anonymous")
      val inputStream = InputStream[IO](bufSize = 1024)
      val chatClientService = ChatClientService(name, inputStream, chatServiceFs2Grpc)
      chatClientService.start
    }.as(ExitCode.Success)
}
