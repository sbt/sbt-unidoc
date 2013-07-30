sbt-unidoc
==========

sbt plugin to unify scaladoc/javadoc across multiple projects.

how to add this plugin
----------------------

For sbt 0.12 add the following to your `project/unidoc.sbt`:

```scala
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.1.2")
```

For sbt 0.13 add the following to your `project/unidoc.sbt`:

```scala
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.2.0")
```

how to unify scaladoc
---------------------

1. Import `sbtunidoc.Plugin._`.
2. Add `unidocSettings` to your root project's settings.

If one of your subprojects is defining def macros, add `scalacOptions in (ScalaUnidoc, unidoc) += "-Ymacro-no-expand"` to the root project's setting to temporary halt the macro expansion.

Here's an example:

```scala
import sbt._
import Keys._
import sbtunidoc.Plugin._
import sbtassembly.Plugin._

object Builds extends Build {
  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    version := "0.1-SNAPSHOT",
    organization := "com.example",
    scalaVersion := "2.10.1"
  )
  lazy val rootSettings = buildSettings ++ unidocSettings ++ Seq(
    name := "foo"
    )
  lazy val librarySettings = buildSettings ++ Seq(
    name := "foo-library"
    )
  lazy val appSettings = buildSettings ++ assemblySettings ++ Seq(
    name := "foo-app"
    )
  lazy val root = Project("root", file("."), settings = rootSettings) aggregate(app, library)
  lazy val library = Project("library", file("library"), settings = librarySettings)
  lazy val app = Project("app", file("app"), settings = appSettings) dependsOn(library)
}
```

From the root project, run `unidoc` task:

```
foo> unidoc
...
[info] Generating Scala API documentation for main sources to /unidoc-sample/target/scala-2.10/unidoc...
[info] Scala API documentation generation successful.
[success] Total time: 10 s, completed May 16, 2013 12:57:10 AM
```

A Scala unidoc is created under `crossTarget / "unidoc"` containing entities from all projects under the build.

how to exclude a project
------------------------

1. Import `UnidocKeys._`.
2. Add the name of a child project to `excludedProjects in unidoc in ScalaUnidoc` in the root project's settings.

```scala
  lazy val rootSettings = buildSettings ++ scalaJavaUnidocSettings ++ Seq(
    name := "foo",
    excludedProjects in unidoc in ScalaUnidoc += "app"
    )
```

This will skip Scaladoc for the app project.

how to publish Scala unidoc to Github Pages
-------------------------------------------

Add sbt-site and sbt-ghpages to your `project/plugins.sbt`:

```scala
resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.6.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.0")
```

Then in `project/build.scala` import bunch of things,

```scala
import com.typesafe.sbt.SbtGhPages._
import com.typesafe.sbt.SbtGit.{GitKeys => git}
import com.typesafe.sbt.SbtSite._
import sbtunidoc.Plugin._
```

add `site.settings` and `ghpages.settings` to the root project's settings, and then add `mappings in packageDoc in ScalaUnidoc` to the site's mapping:

```scala
  lazy val rootSettings = buildSettings ++ unidocSettings ++ 
      site.settings ++ ghpages.settings ++ Seq(
    name := "foo",
    git.gitRemoteRepo := "git@github.com:user/foo.git",
    site.addMappingsToSiteDir(mappings in packageDoc in ScalaUnidoc, "latest/api")
  )
```

Here's how to preview and publish it:

```
foo> preview-site
foo> ghpages-push-site
```

how to unify javadoc
--------------------

1. Import `sbtunidoc.Plugin._`.
2. Add `genjavadocSettings` to child projects' settings.
3. Add `javaUnidocSettings` to the root project's settings.

```scala
import sbt._
import Keys._
import sbtunidoc.Plugin._
import sbtassembly.Plugin._

object Builds extends Build {
  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    version := "0.1-SNAPSHOT",
    organization := "com.example",
    scalaVersion := "2.10.1"
  )
  lazy val rootSettings = buildSettings ++ javaUnidocSettings ++ Seq(
    name := "foo"
    )
  lazy val librarySettings = buildSettings ++ 
    genjavadocSettings ++ Seq(
    name := "foo-library"
    )
  lazy val appSettings = buildSettings ++
    genjavadocSettings ++ assemblySettings ++ Seq(
    name := "foo-app"
    )
  lazy val root = Project("root", file("."), settings = rootSettings) aggregate(app, library)
  lazy val library = Project("library", file("library"), settings = librarySettings)
  lazy val app = Project("app", file("app"), settings = appSettings) dependsOn(library)
}
```

`genjavadocSettings` adds a compiler plugin called [genjavadoc][genjavadoc], which generates Java source code into `target/"java"` from Scala source code, so javadoc can be generated. The main benefits of javadoc are having natural documentation for Java API, IDE support, and Java enum support. However, the genjavadoc does not always generate compilable Java code. YMMV.

First `clean` then `compile` all projects (in the above, root aggreates both children), then run `unidoc` task from the root project:

```
foo> clean
[success] Total time: 0 s, completed May 16, 2013 1:13:55 AM
foo> compile
...
foo> unidoc
[info] Generating Java API documentation for main sources to /unidoc-sample/target/javaunidoc...
[warn] Loading source file /unidoc-sample/app/target/java/foo/App$.java...
....
[info] Java API documentation generation successful.
[success] Total time: 1 s, completed May 16, 2013 1:14:12 AM
```

A Java unidoc is created under `target/"javaunidoc"` containing entities from all projects under the build.

how to publish genjavadoc instead of scaladoc
---------------------------------------------

1. Import `sbtunidoc.Plugin._`.
2. Add `genjavadocExtraSettings` to the child projects.

This will substitute the `packageDoc in Compile` with `packageDoc in Genjavadoc` to use the enhanced Javadoc.

how to unify both Scaladoc and Javadoc
--------------------------------------

1. Import `sbtunidoc.Plugin._`.
2. Add `genjavadocSettings` (or `genjavadocExtraSettings`) to child projects' settings.
3. Add `scalaJavaUnidocSettings` to the root project's settings.

This combines both Scala unidoc settings and Java unidoc settings. Run `unidoc` from the root project to execute both.

credits
-------

The original implementation of `unidoc` task was written by Peter Vlugter ([@pvlugter](https://github.com/pvlugter)) for Akka project. I took [Unidoc.scala](https://github.com/akka/akka/blob/05ba6df5acf48eaf447b5898787e63badbe02cf9/project/Unidoc.scala) in akka/akka@ed40dff7d7353c5cb15b7408ec049116081cb1fc and refactored it. sbt-unidoc is Open Source and available under the Apache 2 License.

  [genjavadoc]: https://github.com/typesafehub/genjavadoc
