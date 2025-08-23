package sbtunidoc

import sbt.*
import sbt.Keys.*
import sbt.plugins.JvmPlugin
import PluginCompat.*

object GenJavadocPlugin extends AutoPlugin {
  object autoImport extends GenJavadocKeys {
    lazy val Genjavadoc = config("genjavadoc") extend Compile
  }
  import autoImport._

  override def globalSettings = unidocGenjavadocVersion := "0.19"

  override def requires = JvmPlugin

  override def projectSettings = Seq(
    libraryDependencies += compilerPlugin("com.typesafe.genjavadoc" %% "genjavadoc-plugin" % unidocGenjavadocVersion.value cross CrossVersion.full),
    scalacOptions += Def.uncached(("-P:genjavadoc:out=" + (target.value / "java"))),
  )
}
