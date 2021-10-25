val commonSettings = Seq(
  scalaVersion := "2.11.12"
)

def module(name: String) =
  Project(name, file(name)).settings(
    commonSettings
  ).enablePlugins(
    GenJavadocPlugin
  )

lazy val a = module("a")
lazy val b = module("b")
lazy val c = module("c")

lazy val root = project.in(file(".")).settings(
  commonSettings,
  unidocProjectFilter in (JavaUnidoc, unidoc) := inAnyProject -- inProjects(c)
).enablePlugins(
  JavaUnidocPlugin
).aggregate(
  a, b, c
)

TaskKey[Unit]("check") := {
  if (scala.util.Properties.isJavaAtLeast("11")) {
    assert(file("target/javaunidoc/allclasses.html").isFile)
  } else {
    assert(file("target/javaunidoc/allclasses-frame.html").isFile)
  }
}
