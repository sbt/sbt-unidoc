package sbtunidoc

import sbt.Keys.Classpath
import sbt.{File, ScopeFilter, URL, settingKey, taskKey}
import PluginCompat.*

trait UnidocKeys {
  @transient
  val unidoc                    = taskKey[Seq[File]]("Create unified scaladoc for all aggregates.")
  val unidocAllSources          = taskKey[Seq[Seq[File]]]("All sources.")
  val unidocAllClasspaths       = taskKey[Seq[Classpath]]("All classpaths.")
  val unidocAllAPIMappings      = taskKey[Seq[Map[FileRef, PluginCompat.URI]]]("All API mappings.")
  val unidocScopeFilter         = settingKey[ScopeFilter]("Control sources to be included in unidoc.")
  val unidocProjectFilter       = settingKey[ScopeFilter.ProjectFilter]("Control projects to be included in unidoc.")
  val unidocConfigurationFilter = settingKey[ScopeFilter.ConfigurationFilter]("Control configurations to be included in unidoc.")
}
