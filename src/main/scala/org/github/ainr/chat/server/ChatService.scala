package org.github.ainr.chat.server

import cats.Applicative
import cats.effect.kernel.Temporal
import cats.effect.std.Console
import fs2.Stream
import fs2.concurrent.Topic
import io.grpc.Metadata
import org.github.ainr.chat.{ChatServiceFs2Grpc, StreamData}

object ChatService {

  def apply[F[_]: Applicative: Temporal: Console](
      eventsTopic: Topic[F, StreamData]
  ): ChatServiceFs2Grpc[F, Metadata] = new ChatServiceFs2Grpc[F, Metadata] {

    override def chatStream(
        request: fs2.Stream[F, StreamData],
        ctx: Metadata
    ): fs2.Stream[F, StreamData] =
      events.concurrently(
        request
          .evalTap(data => Console[F].println(s"From request: $data"))
          .evalTap(eventsTopic.publish1)
      )

    val events: Stream[F, StreamData] =
      eventsTopic
        .subscribe(100)
        .evalTap(data => Console[F].println(s"From topic: $data"))
  }
}
