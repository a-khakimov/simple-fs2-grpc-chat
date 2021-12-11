package org.github.ainr.chat

import io.grpc.stub.StreamObserver
import io.grpc.ServerBuilder
import org.github.ainr.chat.StreamData.Event.{ClientLogin, ClientLogout, ClientMessage, ServerShutdown}

import java.util.logging.Logger
import scala.collection.mutable
import scala.concurrent.ExecutionContext

object ChatService {
  def apply(
    implicit
    ec: ExecutionContext
  ): ChatServiceGrpc.ChatService = new ChatServiceGrpc.ChatService {

    private val logger = Logger.getLogger(getClass.getName)

    val users = mutable.Set.empty[StreamObserver[StreamData]]

    override def chatStream(responseObserver: StreamObserver[StreamData]): StreamObserver[StreamData] = {

      logger.info(s"Chat stream for $responseObserver")
      users.add(responseObserver)

      new StreamObserver[StreamData] {
        override def onNext(value: StreamData): Unit = {
          value.event match {
            case ClientLogin(value) =>
              logger.info(s"ClientLogin: ${value.name}")
              users.foreach(_.onNext(StreamData(ClientLogin(value))))
            case ClientLogout(value) =>
              logger.info(s"ClientLogout: ${value.name}")
              users.foreach(_.onNext(StreamData(ClientLogout(value))))
            case ClientMessage(value) =>
              logger.info(s"ClientMessage: ${value.name} - ${value.message}")
              users.foreach(_.onNext(StreamData(ClientMessage(value))))
            case ServerShutdown(value) =>
              logger.info(s"ServerShutdown")
              users.foreach(_.onNext(StreamData(ServerShutdown(value))))
            case unknown =>
              logger.info(s"$unknown")
          }
        }

        override def onError(t: Throwable): Unit = {
          logger.info(s"Error ${t.getMessage}")
          users.remove(responseObserver)
        }

        override def onCompleted(): Unit = {
          logger.info(s"Completed")
          users.remove(responseObserver)
        }
      }
    }
  }
}

object ChatServerApp {

  val ec = ExecutionContext.global

  final def main(args: Array[String]): Unit = {

    val port: Int =
      args
        .headOption
        .map(_.toInt)
        .getOrElse(50053)

    ServerBuilder
      .forPort(port)
      .addService(ChatServiceGrpc.bindService(ChatService(ec), ec))
      .build()
      .start()
      .awaitTermination()
  }
}

