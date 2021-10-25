sbt-unidoc
==========

sbt plugin to unify scaladoc/javadoc across multiple projects.

how to add this plugin
----------------------

For sbt 1.x (requires sbt 1.5.x and above) add the following to your `project/plugins.sbt`:

```scala
addSbtPlugin("com.github.sbt" % "sbt-unidoc" % "0.5.0")
```

**Note**: We changed the organization from `"com.eed3si9n"` to `"com.github.sbt"`.

For older sbt 1.x and sbt 0.13 add the following to your `project/plugins.sbt`:

```scala
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.3")
```

how to unify scaladoc
---------------------

1. Enable `ScalaUnidocPlugin` in your root project's settings.

Note: If one of your subprojects is defining def macros, add `scalacOptions in (ScalaUnidoc, unidoc) += "-Ymacro-expand:none"` to the root project's setting to temporary halt the macro expansion.

Here's an example setup using multi-project build.sbt:

```scala
ThisBuild / organization := "com.example"
ThisBuild / version := "0.1-SNAPSHOT"
ThisBuild / scalaVersion := scalaVersion := "2.12.15"
ThisBuild / autoAPIMappings := true

val library = (project in file("library"))
  .settings(
    name := "foo-library"
  )

val app = (project in file("app"))
  .dependsOn(library)
  .settings(
    name := "foo-app"
  )

val root = (project in file("."))
  .enablePlugins(ScalaUnidocPlugin)
  .aggregate(library, app)
  .settings(
    name := "foo"
  )
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

1. Construct `ScalaUnidoc / unidoc / unidocProjectFilter` in the root project's settings.

```scala
val root = (project in file("."))
  .enablePlugins(ScalaUnidocPlugin)
  .aggregate(library, app)
  .settings(
    name := "foo",
    ScalaUnidoc / unidoc / unidocProjectFilter := inAnyProject -- inProjects(app)
  )
```

This will skip Scaladoc for the app project.

how to include multiple configurations
--------------------------------------

1. Construct `ScalaUnidoc / unidoc / unidocConfigurationFilter` in the root project's settings.

```scala
val root = (project in file("."))
  .enablePlugins(ScalaUnidocPlugin)
  .aggregate(library, app)
  .settings(
    name := "foo",
    TestScalaUnidoc / unidoc / unidocConfigurationFilter := inConfigurations(Compile, Test)
  )
```

Running `test:unidoc` will now create unidoc including both `Compile` and `Test` configuration.

how to publish Scala unidoc to Github Pages
-------------------------------------------

Add sbt-site and sbt-ghpages to your `project/site.sbt`:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.3.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.2")
```

Then in `build.sbt` import `GitKeys`,

```scala
import com.typesafe.sbt.SbtGit.GitKeys._
```

Add `ScalaUnidoc / packageDoc / mappings` to the site's mapping:

```scala
val root = (project in file("."))
  .enablePlugins(ScalaUnidocPlugin, GhpagesPlugin)
  .aggregate(library, app)
  .settings(
    name := "foo",
    ScalaUnidoc / siteSubdirName := "latest/api",
    addMappingsToSiteDir(ScalaUnidoc / packageDoc / mappings, ScalaUnidoc / siteSubdirName),
    gitRemoteRepo := "git@github.com:user/foo.git"
  )
```

Here's how to preview and publish it:

```
foo> previewSite
foo> ghpagesPushSite
```

how to unify javadoc
--------------------

1. Enable `GenJavadocPlugin` in child projects.
2. Enable `JavaUnidocPlugin` in the root project.

```scala
ThisBuild / organization := "com.example"
ThisBuild / version := "0.1-SNAPSHOT"
ThisBuild / scalaVersion := scalaVersion := "2.12.15"
ThisBuild / autoAPIMappings := true

val library = (project in file("library"))
  .enablePlugins(GenJavadocPlugin)
  .settings(
    name := "foo-library"
  )

val app = (project in file("app"))
  .enablePlugins(GenJavadocPlugin)
  .dependsOn(library)
  .settings(
    name := "foo-app"
  )

val root = (project in file("."))
  .enablePlugins(JavaUnidocPlugin)
  .aggregate(library, app)
  .settings(
    name := "foo"
  )
```

`GenJavadocPlugin` adds a compiler plugin called [genjavadoc][genjavadoc], which generates Java source code into `target/java` from Scala source code, so javadoc can be generated. The main benefits of javadoc are having natural documentation for Java API, IDE support, and Java enum support. However, the genjavadoc does not always generate compilable Java code; if you see misbehavior please open an issue with `genjavadoc`.

First `clean` all projects (in the above, root aggreates both children), then run `unidoc` task from the root project:

```
foo> clean
[success] Total time: 0 s, completed May 16, 2013 1:13:55 AM
foo> unidoc
[info] Compiling 10 Scala sources ...
[info] Generating Java API documentation for main sources to /unidoc-sample/target/javaunidoc...
[warn] Loading source file /unidoc-sample/app/target/java/foo/App$.java...
....
[info] Java API documentation generation successful.
[success] Total time: 1 s, completed May 16, 2013 1:14:12 AM
```

Cleaning is necessary since genjavadoc does not track the correspondence between Scala sources and emitted Java files, leading to extraneous Java sources when deleting classes.
A Java unidoc is created under `target/javaunidoc` containing entities from all projects under the build.

how to publish genjavadoc instead of scaladoc
---------------------------------------------

1. Enable `PublishJavadocPlugin` in child projects.

This will substitute the `Compile / packageDoc` with `Genjavadoc / packageDoc` to use the enhanced Javadoc.

how to unify both Scaladoc and Javadoc
--------------------------------------

1. Enable `GenJavadocPlugin` (or `PublishJavadocPlugin`) in child projects.
2. Enable `ScalaUnidocPlugin` and `JavaUnidocPlugin` in the root project.

This combines both Scala unidoc settings and Java unidoc settings. Run `unidoc` from the root project to execute both.

credits
-------

The original implementation of `unidoc` task was written by Peter Vlugter ([@pvlugter](https://github.com/pvlugter)) for Akka project. I took [Unidoc.scala](https://github.com/akka/akka/blob/05ba6df5acf48eaf447b5898787e63badbe02cf9/project/Unidoc.scala) in akka/akka@ed40dff7d7353c5cb15b7408ec049116081cb1fc and refactored it. sbt-unidoc is Open Source and available under the Apache 2 License.

  [genjavadoc]: https://github.com/typesafehub/genjavadoc
