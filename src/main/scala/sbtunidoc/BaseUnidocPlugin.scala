package sbtunidoc

import sbt.Keys._
import sbt._

object BaseUnidocPlugin extends AutoPlugin {
  object autoImport extends UnidocKeys
  import autoImport._

  override def projectSettings = Seq(
    unidoc in Compile := Seq.empty,
    unidoc in Test := Seq.empty
  )

  def commonSettings(sc: Configuration): Seq[sbt.Def.Setting[_]] = Seq(
    doc := Unidoc(streams.value.cacheDirectory, (compilers in unidoc).value, (sources in unidoc).value, (fullClasspath in unidoc).value,
      (scalacOptions in unidoc).value, (javacOptions in unidoc).value, (apiMappings in unidoc).value, (maxErrors in unidoc).value,
      (target in unidoc).value, configuration.value, streams.value),
    compilers in unidoc := (compilers in sc).value,
    sources in unidoc := (unidocAllSources in unidoc).value.flatten,
    scalacOptions in unidoc := (scalacOptions in (sc, doc)).value,
    javacOptions in unidoc := (javacOptions in (sc, doc)).value,
    fullClasspath in unidoc := (unidocAllClasspaths in unidoc).value.flatten.distinct,
    unidocAllClasspaths in unidoc := allClasspathsTask.value,
    apiMappings in unidoc := {
      val all = (unidocAllAPIMappings in unidoc).value
      val allList = all map { _.toList }
      allList.flatten.distinct.toMap
    },
    unidocAllAPIMappings in unidoc := allAPIMappingsTask.value,
    maxErrors in unidoc := (maxErrors in (sc, doc)).value,
    unidocScopeFilter in unidoc := ScopeFilter((unidocProjectFilter in unidoc).value, (unidocConfigurationFilter in unidoc).value),
    unidocProjectFilter in unidoc := inAnyProject,
    // {
    //   val exclude = excludedProjects.value
    //   inAnyProject -- inProjects(buildStructure.value.allProjectRefs filter { p => exclude contains (p.project) }: _*)
    // },
    // excludedProjects in unidoc := Seq(),
    unidocConfigurationFilter in unidoc := inConfigurations(sc)
  )

  lazy val allClasspathsTask = Def.taskDyn {
    val f = (unidocScopeFilter in unidoc).value
    dependencyClasspath.all(f)
  }
  lazy val allAPIMappingsTask = Def.taskDyn {
    val f = (unidocScopeFilter in unidoc).value
    (apiMappings in (Compile, doc)).all(f)
  }
}
