import sbt._
import Keys._
import com.here.bom.Bom

// see https://github.com/spotify/scio/blob/v0.14.9/build.sbt
val scioVersion = "0.14.9"
val beamVersion = "2.60.0"

val guavaVersion = "33.1.0-jre"
val jacksonVersion = "2.15.4"
val magnolifyVersion = "0.7.4"
val nettyVersion = "4.1.100.Final"
val slf4jVersion = "1.7.30"

lazy val beamBom = Bom("org.apache.beam" % "beam-sdks-java-bom" % beamVersion)
lazy val guavaBom = Bom("com.google.guava" % "guava-bom" % guavaVersion)
lazy val jacksonBom = Bom("com.fasterxml.jackson" % "jackson-bom" % jacksonVersion)
lazy val magnolifyBom = Bom("com.spotify" % "magnolify-bom" % magnolifyVersion)
lazy val nettyBom = Bom("io.netty" % "netty-bom" % nettyVersion)
lazy val scioBom = Bom("com.spotify" % "scio-bom" % scioVersion)


val bomSettings = Def.settings(
  beamBom,
  guavaBom,
  jacksonBom,
  magnolifyBom,
  nettyBom,
  dependencyOverrides ++=
    beamBom.key.value.bomDependencies ++
      guavaBom.key.value.bomDependencies ++
      jacksonBom.key.value.bomDependencies ++
      magnolifyBom.key.value.bomDependencies ++
      nettyBom.key.value.bomDependencies
)

lazy val commonSettings = bomSettings ++ Def.settings(
  organization := "ww",
  // Semantic versioning http://semver.org/
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.13.15",
  scalacOptions ++= Seq(
    "-release", "8",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Ymacro-annotations"
  ),
  javacOptions ++= Seq("--release", "8"),
  // add extra resolved and remove exclude if you need kafka
  // resolvers += "confluent" at "https://packages.confluent.io/maven/",
  excludeDependencies += "org.apache.beam" % "beam-sdks-java-io-kafka"
)

lazy val root: Project = project
  .in(file("."))
  .settings(commonSettings)
  .settings(
    name := "api-telemetry-stream-enrichment",
    description := "api-telemetry-stream-enrichment",
    publish / skip := true,
    fork := true,
    run / outputStrategy := Some(OutputStrategy.StdoutOutput),
    libraryDependencies ++= Seq(
      "com.spotify" %% "scio-core" % scioVersion,
      "org.slf4j" % "slf4j-api" % slf4jVersion,
      "org.apache.beam" % "beam-runners-direct-java" % beamVersion % Test,
      "com.spotify" %% "scio-test" % scioVersion % Test,
      "org.slf4j" % "slf4j-simple" % slf4jVersion % Test
    ),
  )

//lazy val repl: Project = project
//  .in(file(".repl"))
//  .settings(commonSettings)
//  .settings(
//    name := "repl",
//    description := "Scio REPL for api-telemetry-stream-enrichment",
//    libraryDependencies ++= Seq(
//      "com.spotify" %% "scio-repl" % scioVersion
//    ),
//    Compile / mainClass := Some("com.spotify.scio.repl.ScioShell"),
//    publish / skip := true,
//    fork := false,
//  )
//  .dependsOn(root)
