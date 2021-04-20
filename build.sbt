
ThisBuild / organization      := "com.colisweb"
ThisBuild / scalaVersion      := "2.12.8"
ThisBuild / scalafmtOnCompile := true
ThisBuild / scalafmtCheck     := true
ThisBuild / scalafmtSbtCheck  := true
ThisBuild / pushRemoteCacheTo := Some(
  MavenCache("local-cache", baseDirectory.value / sys.env.getOrElse("CACHE_PATH", "sbt-cache"))
)
val testKitLibs = Seq(
  "org.scalactic"  %% "scalactic"  % "3.0.7",
  "org.scalatest"  %% "scalatest"  % "3.0.7",
  "org.mockito" %% "mockito-scala" % "1.2.1"
).map(_ % Test)

lazy val root = Project(id = "JRubyAMQPConsumer", base = file("."))
  .settings(moduleName := "root")
  .settings(noPublishSettings: _*)
  .aggregate(core, jruby)
  .dependsOn(core, jruby)

lazy val core =
  project
    .settings(moduleName := "JRubyAMQPConsumer")
    .settings(
      libraryDependencies ++= Seq(
        "com.lightbend.akka" %% "akka-stream-alpakka-amqp" % "0.20"
      ) ++ testKitLibs
    )

lazy val jruby =
  project
    .settings(moduleName := "JRubyAMQPConsumerAdapter")
    .dependsOn(core)

/**
  * Copied from Cats
  */
def noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)