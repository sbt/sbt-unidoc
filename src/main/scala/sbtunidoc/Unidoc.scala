package sbtunidoc

import sbt._
import sbt.Keys._
import sbt.internal.inc.{AnalyzingCompiler, ManagedLoggedReporter}
import sbt.internal.util.Attributed.data
import xsbti.compile.{Compilers, IncToolOptionsUtil}
import xsbti.{FileConverter, Reporter}

object Unidoc {
  import java.io.PrintWriter

  // This is straight out of docTaskSettings in Defaults.scala.
  def apply(cache: File, cs: Compilers, srcs: Seq[File], cp: Classpath,
            sOpts: Seq[String], jOpts: Seq[String], xapis: Map[File, URL], maxErrors: Int,
            out: File, config: Configuration, s: TaskStreams, spm: Seq[xsbti.Position => Option[xsbti.Position]], converter: FileConverter): File = {
    val hasScala = srcs.exists(_.name.endsWith(".scala")) ||
      srcs.exists(_.name.endsWith(".tasty")) || // Condition for Scaladoc 3
      sOpts.contains("-siteroot") // Condition for Scaladoc 3
    val hasJava = srcs.exists(_.name.endsWith(".java"))
    val label = nameForSrc(config.name)
    val reporter = new ManagedLoggedReporter(
      maxErrors,
      s.log,
      foldMappers(spm))
    (hasScala, hasJava) match {
      case (true, _) =>
        val options = sOpts ++ Opts.doc.externalAPI(xapis)
        val runDoc = Doc.scaladoc(label, s.cacheStoreFactory sub "scala", cs.scalac match {
          case ac: AnalyzingCompiler => ac.onArgs(exported(s, "scaladoc"))
        }, Nil)
        runDoc(srcs, data(cp).toList, out, options, maxErrors, s.log)
      case (_, true) =>
        val javadoc =
          sbt.inc.Doc.cachedJavadoc(label, s.cacheStoreFactory sub "java", cs.javaTools)
        javadoc.run(
          srcs.toList.map(f => converter.toVirtualFile(f.toPath)),
          data(cp).toList.map(f => converter.toVirtualFile(f.toPath)),
          converter,
          out.toPath,
          jOpts.toList,
          IncToolOptionsUtil.defaultIncToolOptions(),
          s.log,
          reporter)
      case _ => () // do nothing
    }
    out
  }

  private[this] def exported(w: PrintWriter, command: String): Seq[String] => Unit = args =>
    w.println( (command +: args).mkString(" ") )
  private[this] def exported(s: TaskStreams, command: String): Seq[String] => Unit = args =>
    exported(s.text("export"), command)
  private[this] def foldMappers[A](mappers: Seq[A => Option[A]]) =
    mappers.foldRight({ (p: A) =>
      p
    }) { (mapper, mappers) =>
      { (p: A) =>
        mapper(p).getOrElse(mappers(p))
      }
    }
  def nameForSrc(name: String): String = name match {
    case "compile"|"javaunidoc"|"scalaunidoc" => "main"
    case _ => name
  }
}
