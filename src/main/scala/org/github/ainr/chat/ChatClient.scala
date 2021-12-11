package org.github.ainr.chat

import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import org.github.ainr.chat.StreamData.Event.{ClientLogin, ClientLogout, ClientMessage, ServerShutdown}

import scala.annotation.tailrec
import scala.io.StdIn

object ChatClientApp {

  final def main(args: Array[String]): Unit = {

    val name = args.headOption.getOrElse("Anonymous")

    val channel = ManagedChannelBuilder
      .forAddress("localhost", 50053)
      .usePlaintext
      .build

    val chatClient = ChatServiceGrpc.stub(channel).chatStream(
      new StreamObserver[StreamData] {
        override def onNext(value: StreamData): Unit = {
          value.event match {
            case ClientLogin(value) =>
              println(s"[Login] ${value.name}")
            case ClientLogout(value) =>
              println(s"[Logout] ${value.name}")
            case ClientMessage(value) =>
              println(s"[Message from ${value.name}] ${value.message}")
            case ServerShutdown(_) =>
              println(s"Server shutdown")
            case unknown =>
              println(s"Unknown event - $unknown")
          }
        }

        override def onError(t: Throwable): Unit = println(s"Error ${t.getMessage}")
        override def onCompleted(): Unit = println("Completed")
      }
    )


    def login(): Unit = chatClient.onNext(StreamData(ClientLogin(StreamData.Login(name))))

    def logout(): Unit = chatClient.onNext(StreamData(ClientLogout(StreamData.Logout(name))))

    def sendMessage(message: String): Unit = chatClient.onNext(StreamData(ClientMessage(StreamData.Message(name, message))))

    sys.addShutdownHook(logout())

    @tailrec
    def program(): Unit = {
      val message = StdIn.readLine()
      sendMessage(message)
      program()
    }

    login()
    program()
  }
}
