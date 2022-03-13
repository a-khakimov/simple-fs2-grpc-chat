# Simple fs2-gRPC chat

## What is it?

This is just a simple experimental project where I dealt with [fs2-grpc](https://github.com/typelevel/fs2-grpc).

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
