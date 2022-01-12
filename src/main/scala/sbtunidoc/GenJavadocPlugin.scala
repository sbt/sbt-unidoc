package sbtunidoc

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

object GenJavadocPlugin extends AutoPlugin {
  object autoImport extends GenJavadocKeys {
    lazy val Genjavadoc = config("genjavadoc") extend Compile
  }
  import autoImport._

  override def globalSettings = unidocGenjavadocVersion := "0.18"

  override def requires = JvmPlugin

  override def projectSettings = Seq(
    libraryDependencies += compilerPlugin("com.typesafe.genjavadoc" %% "genjavadoc-plugin" % unidocGenjavadocVersion.value cross CrossVersion.full),
    scalacOptions += ("-P:genjavadoc:out=" + (target.value / "java")))
}
