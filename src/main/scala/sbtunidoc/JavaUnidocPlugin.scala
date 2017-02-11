package sbtunidoc

import sbt.Keys._
import sbt._

object JavaUnidocPlugin extends AutoPlugin {
  object autoImport extends UnidocKeys {
    lazy val JavaUnidoc     = config("javaunidoc") extend Compile
    lazy val TestJavaUnidoc = config("testjavaunidoc") extend Test
  }

  import autoImport._

  override def projectSettings =
    javaUnidocTask(JavaUnidoc, Compile) ++
    javaUnidocTask(TestJavaUnidoc, Test) ++
    inConfig(TestJavaUnidoc)(Seq(
      target in unidoc := target.value / "testjavaunidoc"
    ))

  def javaUnidocTask(c: Configuration, sc: Configuration): Seq[sbt.Def.Setting[_]] =
    inConfig(c)(Defaults.configSettings ++ baseJavaUnidocTasks(sc)) ++ Seq(
      unidoc in sc := Seq((doc in c).value)
    )

  def baseJavaUnidocTasks(sc: Configuration): Seq[sbt.Def.Setting[_]] = baseCommonUnidocTasks(sc) ++ Seq(
    target in unidoc := target.value / "javaunidoc",
    unidocAllSources in unidoc := allJavaSourcesTask.value
  )

  lazy val javaSources: sbt.Def.Initialize[Task[Seq[File]]] = Def.task {
    val compiled = compile.value
    val sourceJavaFiles = sources.value filter {_.getName endsWith ".java"}
    val targetJavaFiles: Seq[File] = (target.value / "java" ** "*.java").get.sorted
    sourceJavaFiles ++ targetJavaFiles
  }

  lazy val allJavaSourcesTask = Def.taskDyn {
    val f = (unidocScopeFilter in unidoc).value
    javaSources.all(f)
  }
}
