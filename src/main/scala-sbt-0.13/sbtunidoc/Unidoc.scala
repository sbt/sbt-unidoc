package sbtunidoc

import sbt._
import sbt.Keys._
import xsbti.Reporter

object Unidoc {
  import java.io.PrintWriter

  // This is straight out of docTaskSettings in Defaults.scala.
  def apply(cache: File, cs: Compiler.Compilers, srcs: Seq[File], cp: Classpath,
            sOpts: Seq[String], jOpts: Seq[String], xapis: Map[File, URL], maxErrors: Int,
            out: File, config: Configuration, s: TaskStreams, spm: Seq[xsbti.Position => Option[xsbti.Position]]): File = {
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
}
