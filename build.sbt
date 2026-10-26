name := "count-me-up"
organization := "name.lorenzani.andrea"
version := "2.0.0"

scalaVersion := "3.3.9"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  guice,
  jdbc,
  evolutions,
  "com.h2database"         % "h2"                 % "2.5.252",
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test
)

scalacOptions ++= Seq(
  "-feature",
  "-Wunused:imports",
  // generated code (twirl templates and the router) is not ours to tidy
  "-Wconf:src=.*(\\.html|/routes):s,src=.*/target/.*:s"
)

Test / fork := true
