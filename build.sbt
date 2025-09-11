lazy val scala212 = "2.12.20"
lazy val scala3 = "3.7.3"
ThisBuild / crossScalaVersions := Seq(scala212, scala3)
ThisBuild / scalaVersion := scala212
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
    (pluginCrossBuild / sbtVersion) := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.5.8"
        case _      => "2.0.0-RC3"
      }
    },
    scriptedSbt := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.11.4"
        case _      => (pluginCrossBuild / sbtVersion).value
      }
    },
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
ThisBuild / description := "sbt plugin to create a unified API document across projects"
ThisBuild / organization := "com.github.sbt"
ThisBuild / homepage := Some(url("https://github.com/sbt/sbt-unidoc"))
ThisBuild / Compile / scalacOptions ++= Seq("-feature", "-deprecation", "-Xlint")
ThisBuild / licenses := List(License.Apache2)
ThisBuild / developers := List(
  Developer(
    "eed3si9n",
    "Eugene Yokota",
    "@eed3si9n",
    url("https://github.com/eed3si9n")
  )
)
ThisBuild / dynverSonatypeSnapshots := true
Compile / scalacOptions ++= {
  // https://github.com/sbt/sbt/issues/8220
  if (scalaBinaryVersion.value == "2.12")
    Seq("-Wconf:cat=unused-nowarn:s")
  else
    Nil
}
