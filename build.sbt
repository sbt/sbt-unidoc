lazy val commonSettings = Seq(
  organization in ThisBuild := "com.eed3si9n"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    sbtPlugin := true,
    crossSbtVersions := Vector("0.13.18", "1.2.8"),
    name := "sbt-unidoc",
    description := "sbt plugin to create a unified API document across projects",
    licenses := Seq("Apache License v2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    scalacOptions := Seq("-deprecation", "-unchecked"),
    publishArtifact in (Compile, packageBin) := true,
    publishArtifact in (Test, packageBin) := false,
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := true,
    publishMavenStyle := false,
    bintrayOrganization := Some("sbt"),
    bintrayRepository := "sbt-plugin-releases",
    scriptedSettings,
    scriptedLaunchOpts ++= Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value),
    scriptedBufferLog := false
  )
