inThisBuild(
  List(
    scalaVersion := "3.1.2",
    organizationName := "github.com/2m/rbr-favorites-sync",
    startYear := Some(2022),
    licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
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
  .enablePlugins(AutomateHeaderPlugin)

lazy val ui = project
  .dependsOn(core)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "18.0.2-R29",
      "org.ini4j"    % "ini4j"   % "0.5.4"
    ),
    nativeImageVersion := "20.3.6",
    nativeImageJvm := "graalvm-java11",
    nativeImageOptions ++= Seq("--verbose", "--no-fallback", "-H:+AllowIncompleteClasspath"),
    nativeImageAgentOutputDir := (Compile / resourceDirectory).value / "META-INF" / "native-image" / organization.value / name.value,
    fork := true // to avoid javafx double init problems
  )
  .enablePlugins(AutomateHeaderPlugin, NativeImagePlugin)
