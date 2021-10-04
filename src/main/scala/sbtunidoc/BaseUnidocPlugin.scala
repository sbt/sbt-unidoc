package sbtunidoc

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

/** Provides default settings for unidoc plugins.
  *
  * There's no reason to enable this plugin directly. See [[ScalaUnidocPlugin]] or [[JavaUnidocPlugin]] instead.
  */
object BaseUnidocPlugin extends AutoPlugin {
  object autoImport extends UnidocKeys
  import autoImport._

  override def projectSettings = Seq(
    Compile / unidoc := Seq.empty,
    Test / unidoc := Seq.empty
  )

  override def requires = JvmPlugin

  def baseUnidocSettings(sc: Configuration): Seq[sbt.Def.Setting[_]] = Seq(
    doc := Unidoc(streams.value.cacheDirectory, (unidoc / compilers).value, (unidoc / sources).value, (unidoc / fullClasspath).value,
      (unidoc / scalacOptions).value, (unidoc / javacOptions).value, (unidoc / apiMappings).value, (unidoc / maxErrors).value,
      (unidoc / target).value, configuration.value, streams.value, (unidoc / sourcePositionMappers).value, fileConverter.value),
    unidoc / compilers := (sc / compilers).value,
    unidoc / sources := (unidoc / unidocAllSources).value.flatten.sortBy { _.getAbsolutePath },
    unidoc / scalacOptions := (sc / doc / scalacOptions).value,
    unidoc / javacOptions := (sc / doc / javacOptions).value,
    unidoc / fullClasspath := (unidoc / unidocAllClasspaths).value.flatten.distinct.sortBy { _.data.getName },
    unidoc / unidocAllClasspaths := allClasspathsTask.value,
    unidoc / apiMappings := {
      val all = (unidoc / unidocAllAPIMappings).value
      val allList = all map { _.toList }
      allList.flatten.distinct.toMap
    },
    unidoc / unidocAllAPIMappings := allAPIMappingsTask.value,
    unidoc / maxErrors := (sc / doc / maxErrors).value,
    unidoc / unidocScopeFilter := ScopeFilter((unidoc / unidocProjectFilter).value, (unidoc / unidocConfigurationFilter).value),
    unidoc / unidocProjectFilter := inAnyProject,
    unidoc / unidocConfigurationFilter := inConfigurations(sc)
  )

  lazy val allClasspathsTask = Def.taskDyn {
    val f = (unidoc / unidocScopeFilter).value
    dependencyClasspath.all(f)
  }
  lazy val allAPIMappingsTask = Def.taskDyn {
    val f = (unidoc / unidocScopeFilter).value
    (Compile / doc / apiMappings).all(f)
  }
}
