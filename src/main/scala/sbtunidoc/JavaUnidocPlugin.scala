package sbtunidoc

import sbt._
import Keys._
import BaseUnidocPlugin.autoImport._

/** Generates unified javadoc documentation.
  *
  * This plugin must be enabled on the aggregating project. Children projects must enable [[GenJavadocPlugin]].
  */
object JavaUnidocPlugin extends AutoPlugin {
  object autoImport {
    lazy val JavaUnidoc     = config("javaunidoc") extend Compile
    lazy val TestJavaUnidoc = config("testjavaunidoc") extend Test
  }

  import autoImport._

  override def requires = BaseUnidocPlugin

  override def projectSettings =
    javaUnidocTask(JavaUnidoc, Compile) ++
    javaUnidocTask(TestJavaUnidoc, Test) ++
    inConfig(TestJavaUnidoc)(Seq(
      unidoc/ target := target.value / "testjavaunidoc"
    ))

  def javaUnidocTask(c: Configuration, sc: Configuration): Seq[sbt.Def.Setting[_]] =
    inConfig(c)(Defaults.configSettings ++ baseJavaUnidocTasks(sc)) ++ Seq(
      sc / unidoc ++= Seq((c / doc).value)
    )

  def baseJavaUnidocTasks(sc: Configuration): Seq[sbt.Def.Setting[_]] = BaseUnidocPlugin.baseUnidocSettings(sc) ++ Seq(
    unidoc / target := target.value / "javaunidoc",
    unidoc / unidocAllSources := allJavaSourcesTask.value
  )

  lazy val javaSources: sbt.Def.Initialize[Task[Seq[File]]] = Def.task {
    val compiled = compile.value
    val sourceJavaFiles = sources.value filter {_.getName endsWith ".java"}
    val targetJavaFiles: Seq[File] = (target.value / "java" ** "*.java").get().sorted
    sourceJavaFiles ++ targetJavaFiles
  }

  lazy val allJavaSourcesTask = Def.taskDyn {
    val f = (unidoc / unidocScopeFilter).value
    javaSources.all(f)
  }
}
