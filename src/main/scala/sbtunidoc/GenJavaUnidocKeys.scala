package sbtunidoc

import sbt.settingKey

trait GenJavaUnidocKeys {
  val unidocGenjavadocVersion = settingKey[String]("Version of the genjavadoc compiler plugin.")
}
