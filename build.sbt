ThisBuild / organization      := "com.colisweb"
ThisBuild / scalaVersion      := "2.12.8"
ThisBuild / scalafmtOnCompile := true
ThisBuild / scalafmtCheck     := true
ThisBuild / scalafmtSbtCheck  := true

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
        "com.lightbend.akka"    %% "akka-stream-alpakka-amqp" % "0.20"
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

inThisBuild(
  List(
    credentials += Credentials(Path.userHome / ".bintray" / ".credentials"),
    licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
    homepage := Some(url("https://github.com/Colisweb/JRubyAMQPConsumer")),
    bintrayOrganization := Some("colisweb"),
    bintrayReleaseOnPublish := true,
    publishMavenStyle := true,
    pomExtra := (
      <scm>
        <url>git@github.com:Colisweb/JRubyAMQPConsumer.git</url>
        <connection>scm:git:git@github.com:Colisweb/JRubyAMQPConsumer.git</connection>
      </scm>
        <developers>
          <developer>
            <id>guizmaii</id>
            <name>Jules Ivanic</name>
            <url>https://www.colisweb.com</url>
          </developer>
          <developer>
            <id>FlorianDoublet</id>
            <name>Florian Doublet</name>
            <url>https://www.colisweb.com</url>
          </developer>
        </developers>
      )
  )
)