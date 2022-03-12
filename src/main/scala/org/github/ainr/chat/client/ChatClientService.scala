package org.github.ainr.chat.client

import cats.effect.kernel.Concurrent
import cats.effect.std.Console
import fansi.{Bold, Color}
import fs2.{INothing, Pipe}
import io.grpc.Metadata
import org.github.ainr.chat.StreamData.Event.{ClientLogin, ClientLogout, ClientMessage, ServerShutdown}
import org.github.ainr.chat.StreamData.{Login, Message}
import org.github.ainr.chat.{ChatServiceFs2Grpc, StreamData}

trait ChatClientService[F[_]] {
  def start: F[Unit]
}

object ChatClientService {

  def apply[F[_]: Concurrent](
    clientName: String,
    inputStream: InputStream[F],
    chatService: ChatServiceFs2Grpc[F, Metadata],
  )(
    implicit
    console: Console[F]
  ): ChatClientService[F] = new ChatClientService[F] {

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

    private def processEvent(
      implicit
      console: Console[F]
    ): Pipe[F, StreamData, INothing] = _.foreach { data =>
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
