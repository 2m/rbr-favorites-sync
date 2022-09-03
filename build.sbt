inThisBuild(
  List(
    scalaVersion := "3.1.2"
  )
)

lazy val core = project
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core"           % "3.7.6",
      "com.softwaremill.sttp.client3" %% "circe"          % "3.7.6",
      "com.softwaremill.sttp.client3" %% "scribe-backend" % "3.7.6",
      "io.circe"                      %% "circe-generic"  % "0.15.0-M1",
      "org.typelevel"                 %% "cats-core"      % "2.8.0"
    )
  )

lazy val ui = project
  .dependsOn(core)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "18.0.2-R29"
    ),
    // to avoid javafx double init problems
    fork := true
  )
