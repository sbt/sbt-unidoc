val commonSettings = Seq(
  scalaVersion := "2.12.21"
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
  name := "javaunidoc",
  JavaUnidoc / unidoc / unidocProjectFilter := inAnyProject -- inProjects(c),
  TaskKey[Unit]("check") := {
    val docs = (JavaUnidoc / unidoc / target).value
    if (scala.util.Properties.isJavaAtLeast("17")) {
      assert((docs / "allclasses-index.html").isFile)
    } else if (scala.util.Properties.isJavaAtLeast("11")) {
      assert((docs / "allclasses.html").isFile)
    } else {
      assert((docs / "allclasses-frame.html").isFile)
    }
  }
).enablePlugins(
  JavaUnidocPlugin
).aggregate(
  a, b, c
)
