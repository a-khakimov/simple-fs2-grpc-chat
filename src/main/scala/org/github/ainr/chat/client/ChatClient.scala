package org.github.ainr.chat.client

import cats.effect.kernel.Concurrent
import cats.effect.std.Console
import fansi.{Bold, Color}
import fs2.{INothing, Pipe}
import io.grpc.Metadata
import org.github.ainr.chat.StreamData.Event.{ClientLogin, ClientLogout, ClientMessage, ServerShutdown}
import org.github.ainr.chat.StreamData.{Login, Message}
import org.github.ainr.chat.{ChatServiceFs2Grpc, StreamData}

trait ChatClient[F[_]] {
  def start: F[Unit]
}

object ChatClient {

  def apply[
    F[_]
    : Concurrent
    : Console
  ](
    clientName: String,
    inputStream: InputStream[F],
    chatService: ChatServiceFs2Grpc[F, Metadata],
  ): ChatClient[F] = new ChatClient[F] {

    private val grpcMetaData = new Metadata()

    override def start: F[Unit] = {
      chatService
        .chatStream(
          login(clientName) ++ inputStream.read.through(handleInput(clientName)),
          grpcMetaData
        )
        .through(processEvent)
        .compile
        .drain
    }

    private def login(clientName: String): fs2.Stream[F, StreamData] =
      fs2.Stream(StreamData(ClientLogin(Login(clientName))))

    private def processEvent: Pipe[F, StreamData, INothing] = _.foreach { data =>
      data.event match {
        case event: ClientLogin =>
          Console[F].println(s"${Color.Green(event.value.name).overlay(Bold.On)} entered the chat.")
        case event: ClientLogout =>
          Console[F].println(s"${Color.Blue(event.value.name).overlay(Bold.On)} left the chat.")
        case event: ClientMessage =>
          Console[F].println(s"${Color.LightGray(s"${event.value.name}:").overlay(Bold.On)} ${event.value.message}")
        case _: ServerShutdown =>
          Console[F].println(s"${Color.LightRed("Server shutdown")}")
        case unknown =>
          Console[F].println(s"${Color.Red("Unknown event:")} $unknown")
      }
    }

    private def handleInput(clientName: String): Pipe[F, String, StreamData] = _.map {
      text => StreamData(
        ClientMessage(
          Message(
            name = clientName,
            message = text
          )
        )
      )
    }
  }
}
