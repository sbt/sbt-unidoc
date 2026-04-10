lazy val scala212 = "2.12.21"
lazy val scala3 = "3.8.2"
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
        case _      => "2.0.0-RC11"
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
ThisBuild / scalacOptions ++= Seq("-feature", "-deprecation", "-Xlint")
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
scalacOptions ++= {
  if (scalaBinaryVersion.value == "2.12")
    Seq("-release:8")
  else
    Nil
}
