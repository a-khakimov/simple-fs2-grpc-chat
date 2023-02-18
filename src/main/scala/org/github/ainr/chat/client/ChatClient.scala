package org.github.ainr.chat.client

import cats.effect.kernel.Concurrent
import cats.effect.std.Console
import fansi.{Bold, Color}
import fs2.Pipe
import io.grpc.Metadata
import org.github.ainr.chat.Events.Event.{
  ClientLogin,
  ClientLogout,
  ClientMessage,
  ServerShutdown
}
import org.github.ainr.chat.Events.{Login, Message}
import org.github.ainr.chat.{ChatServiceFs2Grpc, Events}

trait ChatClient[F[_]] {
  def start: F[Unit]
}

object ChatClient {

  def apply[F[_]: Concurrent: Console](
      clientName: String,
      inputStream: InputStream[F],
      chatService: ChatServiceFs2Grpc[F, Metadata]
  ): ChatClient[F] = new ChatClient[F] {

    private val grpcMetaData = new Metadata() // empty

    override def start: F[Unit] = {
      chatService
        .eventsStream(
          login(clientName) ++ inputStream.read.through(inputToEvent),
          grpcMetaData
        )
        .through(processEventsFromServer)
        .through(writeToConsole)
        .compile
        .drain
    }

    private def login(clientName: String): fs2.Stream[F, Events] =
      fs2.Stream(Events(ClientLogin(Login(clientName))))

    private def processEventsFromServer: Pipe[F, Events, String] =
      _.map { data =>
        data.event match {
          case event: ClientLogin   => s"${Color.Green(event.value.name).overlay(Bold.On)} entered the chat."
          case event: ClientLogout  => s"${Color.Blue(event.value.name).overlay(Bold.On)} left the chat."
          case event: ClientMessage => s"${Color.LightGray(s"${event.value.name}:").overlay(Bold.On)} ${event.value.message}"
          case _: ServerShutdown    => s"${Color.LightRed("Server shutdown")}"
          case unknown              => s"${Color.Red("Unknown event:")} $unknown"
        }
      }

    private def writeToConsole: Pipe[F, String, Nothing] =
      _.foreach(Console[F].println)

    private def inputToEvent: Pipe[F, String, Events] =
      _.map { text =>
        Events(
          ClientMessage(
            Message(name = clientName, message = text)
          )
        )
      }
  }
}
