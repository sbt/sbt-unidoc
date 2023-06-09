ThisBuild / scalaVersion := "2.12.18"
ThisBuild / version := {
  val orig = (ThisBuild / version).value
  if (orig.endsWith("-SNAPSHOT")) "0.5.0-SNAPSHOT"
  else orig
}

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-unidoc",
    scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value),
    scriptedBufferLog := false,
    // sbt-unidoc requires sbt 1.5.0 and up
    pluginCrossBuild / sbtVersion := "1.5.0",
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
ThisBuild / description := "sbt plugin to create a unified API document across projects"
ThisBuild / organization := "com.github.sbt"
ThisBuild / homepage := Some(url("https://github.com/sbt/sbt-unidoc"))
ThisBuild / Compile / scalacOptions := Seq("-feature", "-deprecation", "-Xlint")
ThisBuild / licenses := List("Apache License v2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))
ThisBuild / developers := List(
  Developer(
    "eed3si9n",
    "Eugene Yokota",
    "@eed3si9n",
    url("https://github.com/eed3si9n")
  )
)
ThisBuild / pomIncludeRepository := { _ =>
  false
}
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true
ThisBuild / dynverSonatypeSnapshots := true
