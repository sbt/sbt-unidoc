package sbtunidoc

import sbt.*
import sbt.Keys.*
import sbt.internal.inc.{AnalyzingCompiler, ManagedLoggedReporter}
// import sbt.internal.util.Attributed.data
import sbt.internal.inc.{CompilerArguments, CompileOutput}
import xsbti.compile.{Compilers, IncToolOptionsUtil}
import xsbti.{FileConverter, PathBasedFile, VirtualFile}
import PluginCompat.*

object Unidoc {
  import java.io.PrintWriter

  // This is straight out of docTaskSettings in Defaults.scala.
  def apply(cache: File, cs: Compilers, srcs: Seq[File], cp: Classpath,
            sOpts: Seq[String], jOpts: Seq[String],
            xapis: Map[FileRef, PluginCompat.URI],
            maxErrors: Int,
            out: File, config: Configuration, s: TaskStreams, spm: Seq[xsbti.Position => Option[xsbti.Position]], converter: FileConverter): File = {
    implicit val conv: FileConverter = converter
    val hasScala = srcs.exists(_.name.endsWith(".scala")) ||
      srcs.exists(_.name.endsWith(".tasty")) || // Condition for Scaladoc 3
      sOpts.contains("-siteroot") // Condition for Scaladoc 3
    val hasJava = srcs.exists(_.name.endsWith(".java"))
    // val label = nameForSrc(config.name)
    val reporter = new ManagedLoggedReporter(
      maxErrors,
      s.log,
      foldMappers(spm))
    (hasScala, hasJava) match {
      case (true, _) =>
        val xapisFiles = xapis.toSeq.map {
          case (k, v) => toFile(k) -> v
        }
        val options = sOpts ++ Opts.doc.externalAPI(xapisFiles)
        val scalac = cs.scalac match {
          case ac: AnalyzingCompiler => ac.onArgs(exported(s, "scaladoc"))
        }
        val scaladocSrcs = srcs
        // todo: cache this
        if (scaladocSrcs.nonEmpty) {
          IO.delete(out)
          IO.createDirectory(out)
          // use PlainVirtualFile since Scaladoc currently doesn't handle actual VirtualFiles
          scalac.doc(
            scaladocSrcs.map(_.toPath()).map(new sbt.internal.inc.PlainVirtualFile(_)),
            cp.map(toNioPath).map(new sbt.internal.inc.PlainVirtualFile(_)),
            converter,
            out.toPath(),
            options,
            maxErrors,
            s.log,
          )
        }
        else ()
      case (_, true) =>
        val javaSourcesOnly: VirtualFile => Boolean = _.id.endsWith(".java")
        val classpath = cp.map(toNioPath).map(converter.toVirtualFile)
        cs.javaTools.javadoc.run(
          srcs.toArray
            .map { x =>
              converter.toVirtualFile(x.toPath)
            }
            .filter(javaSourcesOnly),
          JavaCompilerArguments(Nil, classpath.toList, jOpts.toList).toArray,
          CompileOutput(out.toPath),
          IncToolOptionsUtil.defaultIncToolOptions(),
          reporter,
          s.log,
        )
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

  object JavaCompilerArguments {
    def apply(
        sources: List[VirtualFile],
        classpath: List[VirtualFile],
        options: List[String]
    ): List[String] = {
      val cp = classpath map {
        case x: PathBasedFile => x.toPath
      }
      val sources1 = sources map {
        case x: PathBasedFile => x.toPath
      }
      val classpathOption = List("-classpath", CompilerArguments.absString(cp))
      options ::: classpathOption ::: CompilerArguments.abs(sources1)
    }
  }
}
