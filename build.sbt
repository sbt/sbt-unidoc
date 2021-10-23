lazy val commonSettings = inThisBuild(
    List(
      organization := "com.github.sbt",
      homepage := Some(url("https://github.com/sbt/sbt-unidoc")),
      licenses := Seq("Apache License v2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
      developers := List(
        Developer(
          "eed3si9n",
          "Eugene Yokota",
          "@eed3si9n",
          url("https://github.com/eed3si9n")
        )
      )
    )
  ) ++ Seq(
    scalaVersion := "2.12.15"
  )

lazy val root = (project in file(".")).
  enablePlugins(SbtPlugin).
  settings(commonSettings: _*).
  settings(
    name := "sbt-unidoc",
    description := "sbt plugin to create a unified API document across projects",
    scalacOptions := Seq("-deprecation", "-unchecked"),
    Compile / packageBin / publishArtifact := true,
    Test / packageBin / publishArtifact := false,
    Compile / packageDoc / publishArtifact := false,
    Compile / packageSrc / publishArtifact := true,
    scriptedLaunchOpts ++= Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value),
    scriptedBufferLog := false
  )
