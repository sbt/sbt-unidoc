package sbtunidoc

import sbt._
import sbt.Keys._

/** Publishes javadoc artifacts rather than scaladoc ones. */
object PublishJavadocPlugin extends AutoPlugin {
  override def requires = GenJavadocPlugin

  override def projectSettings = genjavadocExtraTask(GenJavadocPlugin.autoImport.Genjavadoc, Compile)

  def genjavadocExtraTask(c: Configuration, sc: Configuration): Seq[sbt.Def.Setting[_]] =
    inConfig(c)(Defaults.configSettings ++ baseGenjavadocExtraTasks(sc)) ++ Seq(
      sc / packageDoc := (c / packageDoc).value
    )

  def baseGenjavadocExtraTasks(sc: Configuration): Seq[sbt.Def.Setting[_]] = Seq(
    packageDoc / artifactName := { (sv, mod, art) => "" + mod.name + "_" + sv.binary + "-" + mod.revision + "-javadoc.jar" },
    sources := {
      (sc / compile).value
      (target.value / "java" ** "*.java").get ++ (sc / sources).value.filter(_.getName.endsWith(".java"))
    },
    doc / javacOptions := (sc / doc / javacOptions).value
  )
}
