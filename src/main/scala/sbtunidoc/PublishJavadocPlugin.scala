package sbtunidoc

import sbt.*
import sbt.Keys.*
import PluginCompat.*

/** Publishes javadoc artifacts rather than scaladoc ones. */
object PublishJavadocPlugin extends AutoPlugin {
  override def requires = GenJavadocPlugin

  override def projectSettings = genjavadocExtraTask(GenJavadocPlugin.autoImport.Genjavadoc, Compile)

  def genjavadocExtraTask(c: Configuration, sc: Configuration): Seq[sbt.Def.Setting[?]] =
    inConfig(c)(Defaults.configSettings ++ baseGenjavadocExtraTasks(sc)) ++ Seq(
      sc / packageDoc := Def.uncached((c / packageDoc).value),
    )

  def baseGenjavadocExtraTasks(sc: Configuration): Seq[sbt.Def.Setting[?]] = Seq(
    packageDoc / artifactName := { (sv, mod, art) => "" + mod.name + "_" + sv.binary + "-" + mod.revision + "-javadoc.jar" },
    sources := Def.uncached {
      (sc / compile).value
      (target.value / "java" ** "*.java").get() ++ (sc / sources).value.filter(_.getName.endsWith(".java"))
    },
    doc / javacOptions := Def.uncached((sc / doc / javacOptions).value),
  )
}
