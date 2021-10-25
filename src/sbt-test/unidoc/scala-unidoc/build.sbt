val commonSettings = Seq(
  scalaVersion := "2.11.12"
)

lazy val a = project.settings(
  commonSettings
)

lazy val b = project.settings(
  commonSettings
)

lazy val c = project.settings(
  commonSettings
)

lazy val root = project.in(file(".")).settings(
  commonSettings,
  ScalaUnidoc / unidoc / unidocProjectFilter := inAnyProject -- inProjects(c)
).enablePlugins(
  ScalaUnidocPlugin
).aggregate(
  a, b, c
)
