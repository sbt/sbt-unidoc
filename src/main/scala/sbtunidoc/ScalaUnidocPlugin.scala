package sbtunidoc

import sbt._
import Keys._
import BaseUnidocPlugin.autoImport._

/** Generates unified scaladoc documentation. */
object ScalaUnidocPlugin extends AutoPlugin {
  override def requires = BaseUnidocPlugin

  object autoImport {
    lazy val ScalaUnidoc = config("scalaunidoc") extend Compile
    lazy val TestScalaUnidoc = config("testscalaunidoc") extend Test
  }
  import autoImport._

  override def projectSettings =
    scalaUnidocTask(ScalaUnidoc, Compile) ++
    scalaUnidocTask(TestScalaUnidoc, Test) ++
    inConfig(TestScalaUnidoc)(Seq(
      unidoc / target := crossTarget.value / "testunidoc"
    ))

  def scalaUnidocTask(c: Configuration, sc: Configuration): Seq[sbt.Def.Setting[_]] =
    inConfig(c)(Defaults.configSettings ++ baseScalaUnidocTasks(sc)) ++ Seq(
      sc / unidoc ++= Seq((c / doc).value)
    )

  def baseScalaUnidocTasks(sc: Configuration): Seq[sbt.Def.Setting[_]] = BaseUnidocPlugin.baseUnidocSettings(sc) ++ Seq(
    unidoc / target := crossTarget.value / "unidoc",
    unidoc / unidocAllSources := allScalaSources.value
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
