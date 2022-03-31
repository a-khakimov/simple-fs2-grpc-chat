# Simple fs2-gRPC chat

![](https://a-khakimov.github.io/assets/img/grpc.scala.chat.gif)

## What is it?

This is just a simple experimental project where I dealt with [fs2-grpc](https://github.com/typelevel/fs2-grpc). More details in my [blog](https://a-khakimov.github.io/posts/grpc-scala-chat/).

## How to start?

To begin with, you can try to launch the project and see how it works.

### Run server

```bash
$ sbt "runMain org.github.ainr.chat.server.ChatServerApp"
```

### Run client

```bash
$ sbt "runMain org.github.ainr.chat.client.ChatClientApp Anton"
```
