val featherbedVersion = "0.3.3"
val scalatestVersion = "3.0.5"

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
scalacOptions ++= Seq("-Xfatal-warnings", "-feature", "-language:postfixOps")

lazy val root = (project in file("."))
  .settings(
    organization := "com.gambit",
    name := "slackclient",
    version := sys.env.get("SLACK_CLIENT_VERSION").getOrElse("Test"),
    scalaVersion := "2.12.8",
    scalastyleFailOnWarning	:= true,
    compileScalastyle := scalastyle.in(Compile).toTask("").value,
    fork in Test := true,
    envVars in Test := Map("GAMBIT_CORE_URL" -> "http://test", "GAMBIT_CORE_PORT" -> "0"),
    libraryDependencies ++= Seq(
      "ch.qos.logback"                % "logback-classic"     % "1.1.3"          % Runtime,
      "com.github.slack-scala-client" %% "slack-scala-client" % "0.2.5",
      "com.typesafe.scala-logging"    %% "scala-logging"      % "3.9.2",
      "io.github.finagle"             %% "featherbed-core"    % featherbedVersion,
      "io.github.finagle"             %% "featherbed-circe"   % featherbedVersion,
      "org.scalatest"                 %% "scalatest"          % scalatestVersion % Test,
      "org.slf4j"                     %  "slf4j-api"          % "1.7.25",
    )
  )

// Assembly Config
assemblyJarName in assembly := s"slack-client.jar"
mainClass in assembly := Some("com.gambit.slackclient.Main")

val meta = """META.INF(.)*""".r
assemblyMergeStrategy in assembly := {
  case "BUILD" => MergeStrategy.discard
  case meta(_)  => MergeStrategy.discard
  case other => MergeStrategy.defaultMergeStrategy(other)
}
