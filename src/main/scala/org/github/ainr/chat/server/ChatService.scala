package org.github.ainr.chat.server

import cats.effect.Concurrent
import cats.effect.std.Console
import fs2.Stream
import fs2.concurrent.Topic
import io.grpc.Metadata
import org.github.ainr.chat.{ChatServiceFs2Grpc, Events}

object ChatService {

  def apply[F[_]: Concurrent: Console](
      eventsTopic: Topic[F, Events]
  ): ChatServiceFs2Grpc[F, Metadata] = new ChatServiceFs2Grpc[F, Metadata] {

    val eventsToClients: Stream[F, Events] =
      eventsTopic
        .subscribeUnbounded
        .evalTap(data => Console[F].println(s"From topic: $data"))

    override def eventsStream(
        eventsFromClient: fs2.Stream[F, Events],
        ctx: Metadata
    ): fs2.Stream[F, Events] = {
      eventsToClients.concurrently(
        eventsFromClient
          .evalTap(event => Console[F].println(s"Event from client: $event"))
          .evalMap(eventsTopic.publish1)
      )
    }
  }
}
