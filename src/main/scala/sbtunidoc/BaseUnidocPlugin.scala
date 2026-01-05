package sbtunidoc

import sbt.Keys.*
import sbt.*
import sbt.plugins.JvmPlugin
import PluginCompat.*

/** Provides default settings for unidoc plugins.
  *
  * There's no reason to enable this plugin directly. See [[ScalaUnidocPlugin]] or [[JavaUnidocPlugin]] instead.
  */
object BaseUnidocPlugin extends AutoPlugin {
  object autoImport extends UnidocKeys
  import autoImport._

  override def projectSettings = Seq(
    Compile / unidoc := Def.uncached(Seq.empty),
    Test / unidoc := Def.uncached(Seq.empty),
  )

  override def requires = JvmPlugin

  def baseUnidocSettings(sc: Configuration): Seq[sbt.Def.Setting[?]] = Seq(
    doc := Def.uncached {
      Unidoc(
        streams.value.cacheDirectory,
        (unidoc / compilers).value,
        (unidoc / sources).value,
        (unidoc / fullClasspath).value,
        (unidoc / scalacOptions).value,
        (unidoc / javacOptions).value,
        (unidoc / apiMappings).value,
        (unidoc / maxErrors).value,
        (unidoc / target).value,
        configuration.value,
        streams.value,
        (unidoc / sourcePositionMappers).value,
        fileConverter.value)
    },
    unidoc / compilers := Def.uncached((sc / doc / compilers).value),
    unidoc / sources := Def.uncached((unidoc / unidocAllSources).value.flatten.sortBy { _.getAbsolutePath }),
    unidoc / scalacOptions := Def.uncached((sc / doc / scalacOptions).value),
    unidoc / javacOptions := Def.uncached((sc / doc / javacOptions).value),
    unidoc / fullClasspath := Def.uncached((unidoc / unidocAllClasspaths).value.flatten.distinct.sortBy { x => getName(x.data) }),
    unidoc / unidocAllClasspaths := Def.uncached(allClasspathsTask.value),
    unidoc / apiMappings := Def.uncached {
      val all = (unidoc / unidocAllAPIMappings).value
      val allList = all map { _.toList }
      allList.flatten.distinct.toMap
    },
    unidoc / unidocAllAPIMappings := Def.uncached(allAPIMappingsTask.value),
    unidoc / maxErrors := Def.uncached((sc / doc / maxErrors).value),
    unidoc / unidocScopeFilter := Def.uncached(ScopeFilter((unidoc / unidocProjectFilter).value, (unidoc / unidocConfigurationFilter).value)),
    unidoc / unidocProjectFilter := Def.uncached(inAnyProject),
    unidoc / unidocConfigurationFilter := Def.uncached(inConfigurations(sc))
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
