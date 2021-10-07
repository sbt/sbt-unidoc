lazy val commonSettings = Seq(
  ThisBuild / organization := "com.eed3si9n",
  scalaVersion := "2.12.15"
)

lazy val root = (project in file(".")).
  enablePlugins(SbtPlugin).
  settings(commonSettings: _*).
  settings(
    name := "sbt-unidoc",
    description := "sbt plugin to create a unified API document across projects",
    licenses := Seq("Apache License v2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    scalacOptions := Seq("-deprecation", "-unchecked"),
    Compile / packageBin / publishArtifact := true,
    Test / packageBin / publishArtifact := false,
    Compile / packageDoc / publishArtifact := false,
    Compile / packageSrc / publishArtifact := true,
    publishMavenStyle := false,
    bintrayOrganization := Some("sbt"),
    bintrayRepository := "sbt-plugin-releases",
    scriptedLaunchOpts ++= Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value),
    scriptedBufferLog := false
  )
