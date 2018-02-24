import sbtcrossproject.{crossProject, CrossType}

lazy val a = project

lazy val x = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)

lazy val xJs     = x.js
lazy val xJvm    = x.jvm

lazy val root = project.in(file("."))
  .settings(
    unidocProjectFilter in (ScalaUnidoc, unidoc) := {
      inAnyProject -- inProjects(xJs)
    }
  )
  .enablePlugins(ScalaUnidocPlugin)
  .aggregate(a, xJs, xJvm)
