package sbtunidoc

import sbt.*
import java.io.File
import java.nio.file.{ Path => NioPath }
import xsbti.{ FileConverter }

object PluginCompat {
  type URI = java.net.URL
  type FileRef = java.io.File
  def getName(file: FileRef): String = file.getName()
  def toFile(file: FileRef): java.io.File = file
  def toNioPath(a: Attributed[File])(implicit conv: FileConverter): NioPath =
    a.data.toPath()

  // This adds `Def.uncached(...)`
  implicit class DefOp(singleton: Def.type) {
    def uncached[A1](a: A1): A1 = a
  }
}
