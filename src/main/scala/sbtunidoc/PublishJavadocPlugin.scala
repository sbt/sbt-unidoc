package sbtunidoc

import sbt._
import sbt.Keys._

/** Publishes javadoc artifacts rather than scaladoc ones. */
object PublishJavadocPlugin extends AutoPlugin {
  override def requires = GenJavadocPlugin

  override def projectSettings = genjavadocExtraTask(GenJavadocPlugin.autoImport.Genjavadoc, Compile)

  def genjavadocExtraTask(c: Configuration, sc: Configuration): Seq[sbt.Def.Setting[_]] =
    inConfig(c)(Defaults.configSettings ++ baseGenjavadocExtraTasks(sc)) ++ Seq(
      packageDoc in sc := (packageDoc in c).value
    )

  def baseGenjavadocExtraTasks(sc: Configuration): Seq[sbt.Def.Setting[_]] = Seq(
    artifactName in packageDoc := { (sv, mod, art) => "" + mod.name + "_" + sv.binary + "-" + mod.revision + "-javadoc.jar" },
    sources := {
      (compile in sc).value
      (target.value / "java" ** "*.java").get ++ (sources in sc).value.filter(_.getName.endsWith(".java"))
    },
    javacOptions in doc := (javacOptions in (sc, doc)).value
  )
}
