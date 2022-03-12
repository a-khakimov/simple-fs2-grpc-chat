package org.github.ainr.chat.client

import cats.effect.Sync
import cats.effect.std.Console
import fs2.Stream

trait InputStream[F[_]] {
  def read: Stream[F, String]
}

object InputStream {

  def apply[F[_]: Sync](
    bufSize: Int
  )(
    implicit
    console: Console[F]
  ): InputStream[F] = new InputStream[F] {

    override def read: Stream[F, String] = {
      fs2.io
        .stdinUtf8(bufSize)
        .through(fs2.text.lines)
        .evalTap(erase)
        .filter(_.nonEmpty)
    }

    private def erase: PartialFunction[String, F[Unit]] = {
      _ => console.print("\u001b[1A\u001b[0K")
    }
  }
}
