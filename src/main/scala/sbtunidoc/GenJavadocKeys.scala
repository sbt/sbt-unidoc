package sbtunidoc

import sbt.settingKey

trait GenJavadocKeys {
  val unidocGenjavadocVersion = settingKey[String]("Version of the genjavadoc compiler plugin.")
}
