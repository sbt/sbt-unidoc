package sbtunidoc

import sbt._
import Keys._

object Plugin extends sbt.Plugin {
  lazy val JavaUnidoc = config("javaunidoc") extend Compile
  lazy val ScalaUnidoc = config("scalaunidoc") extend Compile
  lazy val Genjavadoc = config("genjavadoc") extend Compile

  import UnidocKeys._

  object UnidocKeys {
    val unidoc           = TaskKey[Seq[File]]("unidoc", "Create unified scaladoc for all aggregates")
    val excludedProjects = SettingKey[Seq[String]]("unidoc-excluded-projects")
    val allSources       = TaskKey[Seq[Seq[File]]]("unidoc-all-sources")
    val allClasspaths    = TaskKey[Seq[Classpath]]("unidoc-all-classpaths")
  }

  lazy val baseCommonUnidocSettings: Seq[sbt.Project.Setting[_]] = Seq(
    doc <<= (cacheDirectory in unidoc, compileInputs in unidoc, target in unidoc, configuration, streams) map { (cache, in, out, config, s) =>
      Unidoc(cache, in, out, config, s.log)
    },
    compileInputs in unidoc <<= (compileInputs in (Compile, doc), sources in unidoc, fullClasspath in unidoc,
        scalacOptions in unidoc, javacOptions in unidoc) map { (in, srcs, cp0, options, javacOptions) =>
      val cp = cp0 map {_.data}
      in.copy(config = in.config.copy(
        classpath = cp,
        sources = srcs,
        options = options,
        javacOptions = javacOptions))
    },
    cacheDirectory in unidoc <<= cacheDirectory / "unidoc",
    sources in unidoc <<= (allSources in unidoc) map { _.flatten },
    scalacOptions in unidoc <<= scalacOptions in (Compile, doc),
    javacOptions in unidoc <<= javacOptions in (Compile, doc),
    fullClasspath in unidoc <<= (allClasspaths in unidoc) map { _.flatten.distinct },
    allClasspaths in unidoc <<= (thisProjectRef, buildStructure, excludedProjects in unidoc) flatMap Unidoc.allClasspathsTask,
    excludedProjects in unidoc := Seq()
  )
  lazy val baseScalaUnidocSettings: Seq[sbt.Project.Setting[_]] = baseCommonUnidocSettings ++ Seq(
    target in unidoc <<= crossTarget / "unidoc",
    allSources in unidoc <<= (thisProjectRef, buildStructure, excludedProjects in unidoc) flatMap Unidoc.allScalaSourcesTask
  )
  lazy val baseJavaUnidocSettings: Seq[sbt.Project.Setting[_]] = baseCommonUnidocSettings ++ Seq(
    target in unidoc <<= target / "javaunidoc",
    allSources in unidoc <<= (thisProjectRef, buildStructure, excludedProjects in unidoc) flatMap Unidoc.allJavaSourcesTask
  )
  lazy val baseGenjavadocExtraSettings: Seq[sbt.Project.Setting[_]] = Seq(
    artifactName in packageDoc := { (sv, mod, art) => "" + mod.name + "_" + sv.binary + "-" + mod.revision + "-javadoc.jar" },
    sources <<= (target, sources in Compile) map { (t, s) =>
      (t / "java" ** "*.java").get ++ s.filter(_.getName.endsWith(".java"))
    },
    javacOptions in doc <<= javacOptions in (Compile, doc)
  )  
  /** Add this to child projects to generate equivalent java files out of scala files. */
  lazy val genjavadocSettings: Seq[sbt.Project.Setting[_]] = Seq(
    libraryDependencies += compilerPlugin("com.typesafe.genjavadoc" %% "genjavadoc-plugin" % "0.5" cross CrossVersion.full),
    scalacOptions <+= target map (t => "-P:genjavadoc:out=" + (t / "java")))
  /** Add this to child projects to replace packaged javadoc with the genjavadoc. */
  lazy val genjavadocExtraSettings: Seq[sbt.Project.Setting[_]] =
    genjavadocSettings ++
    inConfig(Genjavadoc)(Defaults.configSettings ++ baseGenjavadocExtraSettings) ++ Seq(
      packageDoc in Compile <<= packageDoc in Genjavadoc
    )
  /** Add this to the root project to generate Java unidoc. */
  lazy val javaUnidocSettings: Seq[sbt.Project.Setting[_]] =
    inConfig(JavaUnidoc)(Defaults.configSettings ++ baseJavaUnidocSettings) ++ Seq(
      unidoc <<= (doc in JavaUnidoc) map {Seq(_)})
  /** Add this to the root project to generate Scala unidoc. */
  lazy val scalaUnidocSettings: Seq[sbt.Project.Setting[_]] =
    inConfig(ScalaUnidoc)(Defaults.configSettings ++ baseScalaUnidocSettings) ++ Seq(
      unidoc <<= (doc in ScalaUnidoc) map {Seq(_)})
  /** An alias for scalaUnidocSettings */
  lazy val unidocSettings: Seq[sbt.Project.Setting[_]] = scalaUnidocSettings
  /** Add this to the root project to generate both Scala unidoc and Java unidoc. */
  lazy val scalaJavaUnidocSettings: Seq[sbt.Project.Setting[_]] =
    inConfig(ScalaUnidoc)(Defaults.configSettings ++ baseScalaUnidocSettings) ++ 
    inConfig(JavaUnidoc)(Defaults.configSettings ++ baseJavaUnidocSettings) ++ Seq(
      unidoc <<= (doc in ScalaUnidoc, doc in JavaUnidoc) map { (s, j) => Seq(s, j) })

  object Unidoc {
    // This is straight out of docTaskSettings in Defaults.scala.
    def apply(cache: File, in: Compiler.Inputs, out: File, config: Configuration, log: Logger): File = {
      val srcs = in.config.sources
      val hasScala = srcs.exists(_.name.endsWith(".scala"))
      val hasJava = srcs.exists(_.name.endsWith(".java"))
      val cp = in.config.classpath.toList filterNot (_ == in.config.classesDirectory)
      if(hasScala)
        Doc(in.config.maxErrors, in.compilers.scalac).cached(cache / "scala", nameForSrc(config.name), srcs, cp, out, in.config.options, log)
      else if(hasJava)
        Doc(in.config.maxErrors, in.compilers.javac).cached(cache / "java", nameForSrc(config.name), srcs, cp, out, in.config.javacOptions, log)
      out
    }
    def nameForSrc(name: String): String = name match {
      case "compile"|"javaunidoc"|"scalaunidoc" => "main"
      case _ => name
    }
    def allScalaSourcesTask(projectRef: ProjectRef, structure: Load.BuildStructure, exclude: Seq[String]): Task[Seq[Seq[File]]] = {
      val projects = aggregated(projectRef, structure, exclude)
      projects flatMap { sources in Compile in LocalProject(_) get structure.data } join
    }
    def allJavaSourcesTask(projectRef: ProjectRef, structure: Load.BuildStructure, exclude: Seq[String]): Task[Seq[Seq[File]]] = {
      val projects = aggregated(projectRef, structure, exclude)
      val javaSources = projects flatMap { p =>
        val taskOption: Option[Task[Seq[File]]] = sources in Compile in LocalProject(p) get structure.data
        val targetOption: Option[File] = target in Compile in LocalProject(p) get structure.data
        val targetJavaFiles: Seq[File] = targetOption map { t =>
          (t / "java" ** "*.java").get.sorted
        } getOrElse Nil
        taskOption map { _  map { seq =>
          val sourceJavaFiles: Seq[File] = seq filter (_.getName endsWith ".java")
          sourceJavaFiles ++ targetJavaFiles
        }}
      }
      javaSources.join
    }
    def allClasspathsTask(projectRef: ProjectRef, structure: Load.BuildStructure, exclude: Seq[String]): Task[Seq[Classpath]] = {
      val projects = aggregated(projectRef, structure, exclude)
      projects flatMap { dependencyClasspath in Compile in LocalProject(_) get structure.data } join
    }
    def aggregated(projectRef: ProjectRef, structure: Load.BuildStructure, exclude: Seq[String]): Seq[String] = {
      val aggregate = Project.getProject(projectRef, structure).toSeq.flatMap(_.aggregate)
      aggregate flatMap { ref =>
        if (exclude contains ref.project) Seq.empty
        else ref.project +: aggregated(ref, structure, exclude)
      }
    }    
  }
}
