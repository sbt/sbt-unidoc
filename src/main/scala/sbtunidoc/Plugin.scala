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
  
  lazy val baseCommonUnidocSettings: Seq[sbt.Def.Setting[_]] = Seq(
    doc := Unidoc(streams.value.cacheDirectory, (compilers in unidoc).value, (sources in unidoc).value, (fullClasspath in unidoc).value,
      (scalacOptions in unidoc).value, (javacOptions in unidoc).value, (apiMappings in unidoc).value, (maxErrors in unidoc).value,
      (target in unidoc).value, configuration.value, streams.value),
    compilers in unidoc := (compilers in Compile).value,
    sources in unidoc <<= (allSources in unidoc) map { _.flatten },
    scalacOptions in unidoc := (scalacOptions in (Compile, doc)).value,
    javacOptions in unidoc := (javacOptions in (Compile, doc)).value,
    fullClasspath in unidoc <<= (allClasspaths in unidoc) map { _.flatten.distinct },
    allClasspaths in unidoc <<= (thisProjectRef, buildStructure, excludedProjects in unidoc) flatMap Unidoc.allClasspathsTask,
    excludedProjects in unidoc := Seq(),
    apiMappings in unidoc := (apiMappings in Compile).value,
    maxErrors in unidoc := (maxErrors in (Compile, doc)).value
  )
  lazy val baseScalaUnidocSettings: Seq[sbt.Def.Setting[_]] = baseCommonUnidocSettings ++ Seq(
    target in unidoc := crossTarget.value / "unidoc",
    allSources in unidoc <<= (thisProjectRef, buildStructure, excludedProjects in unidoc) flatMap Unidoc.allScalaSourcesTask
  )
  lazy val baseJavaUnidocSettings: Seq[sbt.Def.Setting[_]] = baseCommonUnidocSettings ++ Seq(
    target in unidoc := target.value / "javaunidoc",
    allSources in unidoc <<= (thisProjectRef, buildStructure, excludedProjects in unidoc) flatMap Unidoc.allJavaSourcesTask
  )
  lazy val baseGenjavadocExtraSettings: Seq[sbt.Def.Setting[_]] = Seq(
    artifactName in packageDoc := { (sv, mod, art) => "" + mod.name + "_" + sv.binary + "-" + mod.revision + "-javadoc.jar" },
    sources <<= (target, sources in Compile) map { (t, s) =>
      (t / "java" ** "*.java").get ++ s.filter(_.getName.endsWith(".java"))
    },
    javacOptions in doc := (javacOptions in (Compile, doc)).value
  )  
  /** Add this to child projects to generate equivalent java files out of scala files. */
  lazy val genjavadocSettings: Seq[sbt.Def.Setting[_]] = Seq(
    libraryDependencies += compilerPlugin("com.typesafe.genjavadoc" %% "genjavadoc-plugin" % "0.5" cross CrossVersion.full),
    scalacOptions <+= target map (t => "-P:genjavadoc:out=" + (t / "java")))
  /** Add this to child projects to replace packaged javadoc with the genjavadoc. */
  lazy val genjavadocExtraSettings: Seq[sbt.Def.Setting[_]] =
    genjavadocSettings ++
    inConfig(Genjavadoc)(Defaults.configSettings ++ baseGenjavadocExtraSettings) ++ Seq(
      packageDoc in Compile <<= packageDoc in Genjavadoc
    )
  /** Add this to the root project to generate Java unidoc. */
  lazy val javaUnidocSettings: Seq[sbt.Def.Setting[_]] =
    inConfig(JavaUnidoc)(Defaults.configSettings ++ baseJavaUnidocSettings) ++ Seq(
      unidoc <<= (doc in JavaUnidoc) map {Seq(_)})
  /** Add this to the root project to generate Scala unidoc. */
  lazy val scalaUnidocSettings: Seq[sbt.Def.Setting[_]] =
    inConfig(ScalaUnidoc)(Defaults.configSettings ++ baseScalaUnidocSettings) ++ Seq(
      unidoc <<= (doc in ScalaUnidoc) map {Seq(_)})
  /** An alias for scalaUnidocSettings */
  lazy val unidocSettings: Seq[sbt.Def.Setting[_]] = scalaUnidocSettings
  /** Add this to the root project to generate both Scala unidoc and Java unidoc. */
  lazy val scalaJavaUnidocSettings: Seq[sbt.Def.Setting[_]] =
    inConfig(ScalaUnidoc)(Defaults.configSettings ++ baseScalaUnidocSettings) ++ 
    inConfig(JavaUnidoc)(Defaults.configSettings ++ baseJavaUnidocSettings) ++ Seq(
      unidoc <<= (doc in ScalaUnidoc, doc in JavaUnidoc) map { (s, j) => Seq(s, j) })

  object Unidoc {
    import java.io.PrintWriter

    // This is straight out of docTaskSettings in Defaults.scala.
    def apply(cache: File, cs: Compiler.Compilers, srcs: Seq[File], cp: Classpath,
      sOpts: Seq[String], jOpts: Seq[String], xapis: Map[File, URL], maxErrors: Int,
      out: File, config: Configuration, s: TaskStreams): File = {
      val hasScala = srcs.exists(_.name.endsWith(".scala"))
      val hasJava = srcs.exists(_.name.endsWith(".java"))
      val label = nameForSrc(config.name)
      val (options, runDoc) =
        if(hasScala)
          (sOpts ++ Opts.doc.externalAPI(xapis), // can't put the .value calls directly here until 2.10.2
            Doc.scaladoc(label, cache / "scala", cs.scalac.onArgs(exported(s, "scaladoc"))))
        else if(hasJava)
          (jOpts,
            Doc.javadoc(label, cache / "java", cs.javac.onArgs(exported(s, "javadoc"))))
        else
          (Nil, RawCompileLike.nop)
      runDoc(srcs, cp map {_.data}, out, options, maxErrors, s.log)
      out
    }
    private[this] def exported(w: PrintWriter, command: String): Seq[String] => Unit = args =>
      w.println( (command +: args).mkString(" ") )
    private[this] def exported(s: TaskStreams, command: String): Seq[String] => Unit = args =>
      exported(s.text("export"), command)
    def nameForSrc(name: String): String = name match {
      case "compile"|"javaunidoc"|"scalaunidoc" => "main"
      case _ => name
    }
    def allScalaSourcesTask(projectRef: ProjectRef, structure: BuildStructure, exclude: Seq[String]): Task[Seq[Seq[File]]] = {
      val projects = aggregated(projectRef, structure, exclude)
      projects flatMap { sources in Compile in LocalProject(_) get structure.data } join
    }
    def allJavaSourcesTask(projectRef: ProjectRef, structure: BuildStructure, exclude: Seq[String]): Task[Seq[Seq[File]]] = {
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
    def allClasspathsTask(projectRef: ProjectRef, structure: BuildStructure, exclude: Seq[String]): Task[Seq[Classpath]] = {
      val projects = aggregated(projectRef, structure, exclude)
      projects flatMap { dependencyClasspath in Compile in LocalProject(_) get structure.data } join
    }
    def aggregated(projectRef: ProjectRef, structure: BuildStructure, exclude: Seq[String]): Seq[String] = {
      val aggregate = Project.getProject(projectRef, structure).toSeq.flatMap(_.aggregate)
      aggregate flatMap { ref =>
        if (exclude contains ref.project) Seq.empty
        else ref.project +: aggregated(ref, structure, exclude)
      }
    }    
  }
}
