package sbtunidoc

import sbt._
import sbt.Keys._
import sbtunidoc.UnidocPlugin.autoImport.{ScalaUnidoc, TestScalaUnidoc}
import sbtunidoc.JavaUnidocPlugin.autoImport.{JavaUnidoc, TestJavaUnidoc}

object ScalaJavaUnidocPlugin extends AutoPlugin {
  object autoImport extends UnidocKeys

  import autoImport._

  override def projectSettings = scalaJavaUnidocTask(ScalaUnidoc, JavaUnidoc, Compile) ++
                                 scalaJavaUnidocTask(TestScalaUnidoc, TestJavaUnidoc, Test)

  def scalaJavaUnidocTask(c1: Configuration, c2: Configuration, sc: Configuration): Seq[sbt.Def.Setting[_]] =
    inConfig(c1)(Defaults.configSettings ++ UnidocPlugin.baseScalaUnidocTasks(sc)) ++
    inConfig(c2)(Defaults.configSettings ++ JavaUnidocPlugin.baseJavaUnidocTasks(sc)) ++ Seq(
      unidoc in sc <<= (doc in c1, doc in c2) map { (s, j) => Seq(s, j) })
}
