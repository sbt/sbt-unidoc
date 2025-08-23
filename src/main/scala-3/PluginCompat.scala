package sbtunidoc

import sbt.*
import java.io.File
import java.nio.file.{ Path => NioPath }
import xsbti.{ FileConverter, HashedVirtualFileRef }

object PluginCompat:
  type URI = java.net.URI
  type FileRef = HashedVirtualFileRef
  def getName(file: HashedVirtualFileRef): String = file.name
  def toNioPath(a: Attributed[HashedVirtualFileRef])(using conv: FileConverter): NioPath =
    conv.toPath(a.data)
  def toNioPath(file: HashedVirtualFileRef)(using conv: FileConverter): NioPath =
    conv.toPath(file)
  inline def toFile(file: HashedVirtualFileRef)(using conv: FileConverter): File =
    toNioPath(file).toFile()
end PluginCompat
