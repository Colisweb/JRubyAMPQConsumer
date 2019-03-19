ThisBuild / organization      := "com.colisweb"
ThisBuild / scalaVersion      := "2.12.8"
ThisBuild / scalafmtOnCompile := true
ThisBuild / scalafmtCheck     := true
ThisBuild / scalafmtSbtCheck  := true

val testKitLibs = Seq(
  "org.scalacheck" %% "scalacheck" % "1.14.0",
  "org.scalactic"  %% "scalactic"  % "3.0.7",
  "org.scalatest"  %% "scalatest"  % "3.0.7",
).map(_ % Test)

lazy val root = Project(id = "JRubyAMPQConsumer", base = file("."))
  .settings(moduleName := "root")
  .settings(noPublishSettings: _*)
  .aggregate(core)
  .dependsOn(core)

lazy val core =
  project
    .settings(moduleName := "JRubyAMPQConsumer")
    .settings(
      libraryDependencies ++= Seq(
        "com.lightbend.akka" %% "akka-stream-alpakka-amqp" % "1.0-M3"
      ) ++ testKitLibs
    )


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
    homepage := Some(url("https://github.com/Colisweb/JRubyAMPQConsumer")),
    bintrayOrganization := Some("colisweb"),
    bintrayReleaseOnPublish := true,
    publishMavenStyle := true,
    pomExtra := (
      <scm>
        <url>git@github.com:Colisweb/JRubyAMPQConsumer.git</url>
        <connection>scm:git:git@github.com:Colisweb/JRubyAMPQConsumer.git</connection>
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