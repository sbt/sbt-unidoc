package sbtunidoc

import sbt._
import Keys._

object Plugin extends sbt.Plugin {
  lazy val JavaUnidoc = config("javaunidoc") extend Compile
  lazy val ScalaUnidoc = config("scalaunidoc") extend Compile
  lazy val TestJavaUnidoc = config("testjavaunidoc") extend Test
  lazy val TestScalaUnidoc = config("testscalaunidoc") extend Test
  lazy val Genjavadoc = config("genjavadoc") extend Compile

  import UnidocKeys._

  object UnidocKeys {
    val unidoc                    = taskKey[Seq[File]]("Create unified scaladoc for all aggregates.")
    val unidocAllSources          = taskKey[Seq[Seq[File]]]("All sources.")
    val unidocAllClasspaths       = taskKey[Seq[Classpath]]("All classpaths.")
    val unidocScopeFilter         = settingKey[ScopeFilter]("Control sources to be included in unidoc.")
    val unidocProjectFilter       = settingKey[ScopeFilter.ProjectFilter]("Control projects to be included in unidoc.")
    val unidocConfigurationFilter = settingKey[ScopeFilter.ConfigurationFilter]("Control configurations to be included in unidoc.")
    val unidocGenjavadocVersion   = settingKey[String]("Version of the genjavadoc compiler plugin.")
  }

  def baseCommonUnidocTasks(sc: Configuration): Seq[sbt.Def.Setting[_]] = Seq(
    doc := Unidoc(streams.value.cacheDirectory, (compilers in unidoc).value, (sources in unidoc).value, (fullClasspath in unidoc).value,
      (scalacOptions in unidoc).value, (javacOptions in unidoc).value, (apiMappings in unidoc).value, (maxErrors in unidoc).value,
      (target in unidoc).value, configuration.value, streams.value),
    compilers in unidoc := (compilers in sc).value,
    sources in unidoc := (unidocAllSources in unidoc).value.flatten,
    scalacOptions in unidoc := (scalacOptions in (sc, doc)).value,
    javacOptions in unidoc := (javacOptions in (sc, doc)).value,
    fullClasspath in unidoc := (unidocAllClasspaths in unidoc).value.flatten.distinct,
    unidocAllClasspaths in unidoc := Unidoc.allClasspathsTask.value,
    apiMappings in unidoc := (apiMappings in sc).value,
    maxErrors in unidoc := (maxErrors in (sc, doc)).value,
    unidocScopeFilter in unidoc := ScopeFilter((unidocProjectFilter in unidoc).value, (unidocConfigurationFilter in unidoc).value),
    unidocProjectFilter in unidoc := inAnyProject,
    // {
    //   val exclude = excludedProjects.value
    //   inAnyProject -- inProjects(buildStructure.value.allProjectRefs filter { p => exclude contains (p.project) }: _*)
    // },
    // excludedProjects in unidoc := Seq(),
    unidocConfigurationFilter in unidoc := inConfigurations(sc),
    unidocGenjavadocVersion in Global := "0.8"
  )
  def baseScalaUnidocTasks(sc: Configuration): Seq[sbt.Def.Setting[_]] = baseCommonUnidocTasks(sc) ++ Seq(
    target in unidoc := crossTarget.value / "unidoc",
    unidocAllSources in unidoc := Unidoc.allScalaSources.value
  )
  def baseJavaUnidocTasks(sc: Configuration): Seq[sbt.Def.Setting[_]] = baseCommonUnidocTasks(sc) ++ Seq(
    target in unidoc := target.value / "javaunidoc",
    unidocAllSources in unidoc := Unidoc.allJavaSourcesTask.value
  )
  def baseGenjavadocExtraTasks(sc: Configuration): Seq[sbt.Def.Setting[_]] = Seq(
    artifactName in packageDoc := { (sv, mod, art) => "" + mod.name + "_" + sv.binary + "-" + mod.revision + "-javadoc.jar" },
    sources <<= (target, sources in sc) map { (t, s) =>
      (t / "java" ** "*.java").get ++ s.filter(_.getName.endsWith(".java"))
    },
    javacOptions in doc := (javacOptions in (sc, doc)).value
  )
  /** Add this to child projects to generate equivalent java files out of scala files. */
  lazy val genjavadocSettings: Seq[sbt.Def.Setting[_]] = Seq(
    libraryDependencies += compilerPlugin("com.typesafe.genjavadoc" %% "genjavadoc-plugin" % unidocGenjavadocVersion.value cross CrossVersion.full),
    scalacOptions <+= target map (t => "-P:genjavadoc:out=" + (t / "java")))
  /** Add this to child projects to replace packaged javadoc with the genjavadoc. */
  lazy val genjavadocExtraSettings: Seq[sbt.Def.Setting[_]] = genjavadocExtraTask(Genjavadoc, Compile)
  def genjavadocExtraTask(c: Configuration, sc: Configuration): Seq[sbt.Def.Setting[_]] =
    genjavadocSettings ++
    inConfig(c)(Defaults.configSettings ++ baseGenjavadocExtraTasks(sc)) ++ Seq(
      packageDoc in sc <<= packageDoc in c
    )

  /** Add this to the root project to generate Java unidoc. */
  lazy val javaUnidocSettings: Seq[sbt.Def.Setting[_]] =
    javaUnidocTask(JavaUnidoc, Compile) ++
    javaUnidocTask(TestJavaUnidoc, Test) ++
    inConfig(TestJavaUnidoc)(Seq(
      target in unidoc := target.value / "testjavaunidoc"
    ))
  def javaUnidocTask(c: Configuration, sc: Configuration): Seq[sbt.Def.Setting[_]] =
    inConfig(c)(Defaults.configSettings ++ baseJavaUnidocTasks(sc)) ++ Seq(
      unidoc in sc := Seq((doc in c).value)
    )

  /** Add this to the root project to generate Scala unidoc. */
  lazy val scalaUnidocSettings: Seq[sbt.Def.Setting[_]] =
    scalaUnidocTask(ScalaUnidoc, Compile) ++
    scalaUnidocTask(TestScalaUnidoc, Test) ++
    inConfig(TestScalaUnidoc)(Seq(
      target in unidoc := crossTarget.value / "testunidoc"
    ))
  def scalaUnidocTask(c: Configuration, sc: Configuration): Seq[sbt.Def.Setting[_]] =
    inConfig(c)(Defaults.configSettings ++ baseScalaUnidocTasks(sc)) ++ Seq(
      unidoc in sc := Seq((doc in c).value)
    )

  /** An alias for scalaUnidocSettings */
  lazy val unidocSettings: Seq[sbt.Def.Setting[_]] = scalaUnidocSettings
  /** Add this to the root project to generate both Scala unidoc and Java unidoc. */
  lazy val scalaJavaUnidocSettings: Seq[sbt.Def.Setting[_]] =
    scalaJavaUnidocTask(ScalaUnidoc, JavaUnidoc, Compile) ++
    scalaJavaUnidocTask(TestScalaUnidoc, TestJavaUnidoc, Test)
  def scalaJavaUnidocTask(c1: Configuration, c2: Configuration, sc: Configuration): Seq[sbt.Def.Setting[_]] =
    inConfig(c1)(Defaults.configSettings ++ baseScalaUnidocTasks(sc)) ++
    inConfig(c2)(Defaults.configSettings ++ baseJavaUnidocTasks(sc)) ++ Seq(
      unidoc in sc <<= (doc in c1, doc in c2) map { (s, j) => Seq(s, j) })

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
    lazy val allScalaSources = Def.taskDyn {
      val f = (unidocScopeFilter in unidoc).value
      sources.all(f)
    }
    lazy val javaSources: sbt.Def.Initialize[Task[Seq[File]]] = Def.task {
      val sourceJavaFiles = sources.value filter {_.getName endsWith ".java"}
      val targetJavaFiles: Seq[File] = (target.value / "java" ** "*.java").get.sorted
      sourceJavaFiles ++ targetJavaFiles
    }
    lazy val allJavaSourcesTask = Def.taskDyn {
      val f = (unidocScopeFilter in unidoc).value
      javaSources.all(f)
    }
    lazy val allClasspathsTask = Def.taskDyn {
      val f = (unidocScopeFilter in unidoc).value
      dependencyClasspath.all(f)
    }
  }
}
