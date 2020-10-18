val finchVersion = "0.27.0"
val circeVersion = "0.10.1"
val scalatestVersion = "3.0.5"
val slickVersion = "3.3.0"
val sttpVersion = "1.6.3"

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
scalacOptions ++= Seq("-Xfatal-warnings", "-feature", "-language:postfixOps")

lazy val root = (project in file("."))
  .settings(
    organization := "com.gambit",
    name := "core",
    version := sys.env.get("GAMBIT_CORE_VERSION").getOrElse("Test"),
    scalaVersion := "2.12.8",
    scalastyleFailOnWarning	:= true,
    compileScalastyle := scalastyle.in(Compile).toTask("").value,
    fork in Test := true,
    envVars in Test := Map(
      "BOT_NAME" -> "test",
      "KARMA_API_URL" -> "test",
      "KARMA_API_PORT" -> "42",
      "USER_API_URL" -> "test",
      "USER_API_PORT" -> "42"
    ),
    libraryDependencies ++= Seq(
      "ch.qos.logback"              %  "logback-classic"                  % "1.1.3"           % Runtime,
      "com.github.finagle"          %% "finchx-core"                      % finchVersion,
      "com.github.finagle"          %% "finchx-circe"                     % finchVersion,
      "com.softwaremill.sttp"       %% "async-http-client-backend-future" % sttpVersion,
      "com.softwaremill.sttp"       %% "core"                             % sttpVersion,
      "com.softwaremill.sttp"       %% "json4s"                           % sttpVersion,
      "com.typesafe.scala-logging"  %% "scala-logging"                    % "3.9.2",
      "com.typesafe.slick"          %% "slick"                            % slickVersion,
      "io.circe"                    %% "circe-generic"                    % circeVersion,
      "net.debasishg"               %% "redisclient"                      % "3.9",
      "org.json4s"                  %% "json4s-native"                    % "3.6.0",
      "org.postgresql"              %  "postgresql"                       % "42.2.5",
      "org.scalamock"               %% "scalamock"                        % "4.1.0"           % Test,
      "org.scalatest"               %% "scalatest"                        % scalatestVersion  % Test
    )
  )

// Assembly Config
assemblyJarName in assembly := s"gambit-core.jar"
mainClass in assembly := Some("com.gambit.core.Main")

val meta = """META.INF(.)*""".r
assemblyMergeStrategy in assembly := {
  case "BUILD" => MergeStrategy.discard
  case meta(_)  => MergeStrategy.discard
  case other => MergeStrategy.defaultMergeStrategy(other)
}
