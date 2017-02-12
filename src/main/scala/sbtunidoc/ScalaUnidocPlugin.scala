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
      target in unidoc := crossTarget.value / "testunidoc"
    ))

  def scalaUnidocTask(c: Configuration, sc: Configuration): Seq[sbt.Def.Setting[_]] =
    inConfig(c)(Defaults.configSettings ++ baseScalaUnidocTasks(sc)) ++ Seq(
      unidoc in sc ++= Seq((doc in c).value)
    )

  def baseScalaUnidocTasks(sc: Configuration): Seq[sbt.Def.Setting[_]] = BaseUnidocPlugin.baseUnidocSettings(sc) ++ Seq(
    target in unidoc := crossTarget.value / "unidoc",
    unidocAllSources in unidoc := allScalaSources.value
  )

  lazy val allScalaSources = Def.taskDyn {
    val f = (unidocScopeFilter in unidoc).value
    sources.all(f)
  }
}
