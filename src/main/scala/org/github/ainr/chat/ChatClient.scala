package org.github.ainr.chat

import cats.Functor
import cats.effect.std.Console
import cats.effect.{ExitCode, IO, IOApp, Resource, Sync}
import cats.implicits.catsSyntaxApplicativeId
import fansi.{Bold, Color}
import fs2.concurrent.Topic
import fs2.concurrent.Topic.Closed
import fs2.grpc.client.ClientOptions
import fs2.{INothing, Pipe, Stream}
import fs2.grpc.syntax.all.fs2GrpcSyntaxManagedChannelBuilder
import io.grpc.netty.shaded.io.grpc.netty.{NegotiationType, NettyChannelBuilder}
import io.grpc.{ManagedChannel, Metadata}
import org.github.ainr.chat.StreamData.Event.{ClientLogin, ClientLogout, ClientMessage, ServerShutdown}
import org.github.ainr.chat.StreamData.{Login, Message}
import org.github.ainr.chat._

import scala.concurrent.duration.DurationInt

object ChatClient extends IOApp {

  def inputStream[F[_]: Sync](
    name: String,
    bufSize: Int
  )(
    implicit
    console: Console[F]
  ): Stream[F, StreamData] = {
    fs2.io.stdinUtf8(bufSize)
      .through(fs2.text.lines)
      .evalTap(_ => console.print("\u001b[1A\u001b[0K"))
      .filter(_.nonEmpty)
      .evalMap { text =>
        StreamData(ClientMessage(Message(name = name, message = text))).pure
      }
  }

  val managedChannelResource: Resource[IO, ManagedChannel] =
    NettyChannelBuilder
      .forAddress("127.0.0.1", 50053)
      .enableRetry()
      .usePlaintext()
      .resource[IO]

  def runProgram[F[_]: Functor](
    chatService: org.github.ainr.chat.ChatServiceFs2Grpc[F, Metadata],
    input: Stream[F, StreamData]
  )(
    implicit
    console: Console[F]
  ): Stream[F, StreamData] = {
    //val request = StreamData(ClientLogin(Login(name = "Hui")))
    def processEvent: Pipe[F, StreamData, INothing] = _.foreach { data =>
      data.event match {
        case event: ClientLogin =>
          console.println(s"${Color.Green(event.value.name).overlay(Bold.On)} entered the chat.")
        case event: ClientLogout =>
          console.println(s"${Color.Blue(event.value.name).overlay(Bold.On)} left the chat.")
        case event: ClientMessage =>
          console.println(s"${Color.LightGray(s"${event.value.name}:").overlay(Bold.On)} ${event.value.message}")
        case _: ServerShutdown =>
          console.println(s"${Color.LightRed("Server shutdown")}")
        case unknown =>
          console.println(s"${Color.Red("Unknown event:")} $unknown")
      }
    }

      chatService
        .chatStream(input, new Metadata())
        .through(processEvent)
    }


  override def run(args: List[String]): IO[ExitCode] = {
    val name = args.headOption.getOrElse("Anonymous")
    managedChannelResource
      .flatMap(
        channel =>
          ChatServiceFs2Grpc.stubResource[IO](
            channel,
            ClientOptions.default
          )
      )
      .use { case chatClient =>
        runProgram[IO](
          chatClient,
          fs2.Stream[IO, StreamData](StreamData(ClientLogin(Login(name)))) ++ inputStream[IO](name, bufSize = 1024)
        )
        .compile
        .drain
      }
      .as(ExitCode.Success)
  }
}
