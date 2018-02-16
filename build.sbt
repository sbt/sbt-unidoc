lazy val commonSettings = Seq(
  git.baseVersion in ThisBuild := "0.4.1-SNAPSHOT",
  organization in ThisBuild := "com.eed3si9n"
)

lazy val root = (project in file(".")).
  enablePlugins(GitVersioning).
  settings(commonSettings: _*).
  settings(
    sbtPlugin := true,
    crossSbtVersions := Vector("0.13.17", "1.0.1"),
    name := "sbt-unidoc",
    description := "sbt plugin to create a unified API document across projects",
    licenses := Seq("Apache License v2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    scalacOptions := Seq("-deprecation", "-unchecked"),
    publishArtifact in (Compile, packageBin) := true,
    publishArtifact in (Test, packageBin) := false,
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := true
  )
