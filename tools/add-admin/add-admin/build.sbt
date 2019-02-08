val slickVersion = "3.3.0"

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
scalacOptions ++= Seq("-Xfatal-warnings", "-feature", "-language:postfixOps", "-deprecation")

lazy val root = (project in file("."))
  .settings(
    organization := "com.gambit.tools",
    name := "admin",
    version := sys.env.get("ADD_ADMIN_VERSION").getOrElse("Test"),
    scalaVersion := "2.12.8",
    scalastyleFailOnWarning	:= true,
    compileScalastyle := scalastyle.in(Compile).toTask("").value,
    fork in Test := true,
    libraryDependencies ++= Seq(
      "ch.qos.logback"              % "logback-classic" % "1.1.3" % Runtime,
      "org.postgresql"              %   "postgresql"    % "42.2.5",
      "com.typesafe.scala-logging"  %% "scala-logging"  % "3.9.2",
      "com.typesafe.slick"          %%  "slick"         % slickVersion
  )
)

// Assembly Config
assemblyJarName in assembly := s"add-admin.jar"
mainClass in assembly := Some("com.gambit.tools.admin.Main")

val meta = """META.INF(.)*""".r
assemblyMergeStrategy in assembly := {
  case "BUILD" => MergeStrategy.discard
  case meta(_)  => MergeStrategy.discard
  case other => MergeStrategy.defaultMergeStrategy(other)
}
