name := "Simple fs2-grpc Chat"

version := "0.1"

scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "gRPC-Chat",
    assembly / assemblyJarName := "gRPC-Chat.jar"
  )

libraryDependencies ++= Seq(
  "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
  "com.lihaoyi" %% "fansi" % "0.4.0",
  "co.fs2" %% "fs2-io" % "3.6.1"
)

enablePlugins(Fs2Grpc)
