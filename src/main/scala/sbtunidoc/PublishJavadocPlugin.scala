package sbtunidoc

import sbt._
import sbt.Keys._

object PublishJavadocPlugin extends AutoPlugin {
  override def requires = GenJavadocPlugin

  override def projectSettings = genjavadocExtraTask(GenJavadocPlugin.autoImport.Genjavadoc, Compile)

  def genjavadocExtraTask(c: Configuration, sc: Configuration): Seq[sbt.Def.Setting[_]] =
    inConfig(c)(Defaults.configSettings ++ baseGenjavadocExtraTasks(sc)) ++ Seq(
      packageDoc in sc <<= packageDoc in c
    )

  def baseGenjavadocExtraTasks(sc: Configuration): Seq[sbt.Def.Setting[_]] = Seq(
    artifactName in packageDoc := { (sv, mod, art) => "" + mod.name + "_" + sv.binary + "-" + mod.revision + "-javadoc.jar" },
    sources <<= (target, sources in sc, compile in sc) map { (t, s, c) =>
      (t / "java" ** "*.java").get ++ s.filter(_.getName.endsWith(".java"))
    },
    javacOptions in doc := (javacOptions in (sc, doc)).value
  )
}
