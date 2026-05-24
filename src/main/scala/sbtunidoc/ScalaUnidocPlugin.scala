package sbtunidoc

import sbt.*
import Keys.*
import BaseUnidocPlugin.autoImport.*
import PluginCompat.*

/** Generates unified scaladoc documentation. */
object ScalaUnidocPlugin extends AutoPlugin {
  override def requires = BaseUnidocPlugin

  object autoImport {
    lazy val ScalaUnidoc = config("scalaunidoc").extend(Compile)
    lazy val TestScalaUnidoc = config("testscalaunidoc").extend(Test)
  }
  import autoImport.*

  override def projectSettings =
    scalaUnidocTask(ScalaUnidoc, Compile) ++
    scalaUnidocTask(TestScalaUnidoc, Test) ++
    inConfig(TestScalaUnidoc)(Seq(
      unidoc / target := crossTarget.value / "testunidoc"
    ))

  def scalaUnidocTask(c: Configuration, sc: Configuration): Seq[sbt.Def.Setting[?]] =
    inConfig(c)(Defaults.configSettings ++ baseScalaUnidocTasks(sc)) ++ Seq(
      sc / unidoc ++= Def.uncached(Seq((c / doc).value))
    )

  def baseScalaUnidocTasks(sc: Configuration): Seq[sbt.Def.Setting[?]] = BaseUnidocPlugin.baseUnidocSettings(sc) ++ Seq(
    unidoc / target := crossTarget.value / "unidoc",
    unidoc / unidocAllSources := Def.uncached(allScalaSources.value)
  )

  lazy val allScalaSources = Def.taskDyn {
    val f = (unidoc / unidocScopeFilter).value
    if(ScalaArtifacts.isScala3(scalaVersion.value)) {
      tastyFiles.all(f) // Since Scaladoc 3 works on TASTy files
    } else {
      sources.all(f)
    }
  }
}
