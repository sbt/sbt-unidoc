sbt-unidoc
==========

sbt plugin to unify scaladoc/javadoc across multiple projects.

how to add this plugin
----------------------

For sbt 0.13 add the following to your `project/unidoc.sbt`:

```scala
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.0")
```

For sbt 0.12, see [sbt-unidoc 0.1.2](https://github.com/sbt/sbt-unidoc/tree/v0.1.2)

how to unify scaladoc
---------------------

1. Add `unidocSettings` to your root project's settings.

Note: If one of your subprojects is defining def macros, add `scalacOptions in (ScalaUnidoc, unidoc) += "-Ymacro-no-expand"` to the root project's setting to temporary halt the macro expansion.

Here's an example setup using multi-project build.sbt:

```scala
val commonSettings = Seq(
    organization := "com.example",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.10.3"
  )

val library = (project in file("library")).
  settings(commonSettings: _*).
  settings(
    name := "foo-library"
  )

val app = (project in file("app")).
  settings(commonSettings: _*).
  settings(
    name := "foo-app"
  ).
  dependsOn(library)

val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(unidocSettings: _*).
  settings(
    name := "foo"
  ).
  aggregate(library, app)
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
2. Construct `unidocProjectFilter in (ScalaUnidoc, unidoc)` in the root project's settings.

```scala
val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(unidocSettings: _*).
  settings(
    name := "foo",
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(app)
  ).
  aggregate(library, app)
```

This will skip Scaladoc for the app project.

how to include multiple configurations
--------------------------------------

1. Import `UnidocKeys._`.
2. Construct `unidocConfigurationFilter in (ScalaUnidoc, unidoc)` in the root project's settings.

```scala
val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(unidocSettings: _*).
  settings(
    name := "foo",
    unidocConfigurationFilter in (TestScalaUnidoc, unidoc) := inConfigurations(Compile, Test),
  ).
  aggregate(library, app)
```

Running `test:unidoc` will now create unidoc including both `Compile` and `Test` configuration.

how to publish Scala unidoc to Github Pages
-------------------------------------------

Add sbt-site and sbt-ghpages to your `project/site.sbt`:

```scala
resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.7.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.2")
```

Then in `build.sbt` import `GitKeys`,

```scala
import com.typesafe.sbt.SbtGit.{GitKeys => git}
```

add `site.settings` and `ghpages.settings` to the root project's settings, and then add `mappings in packageDoc in ScalaUnidoc` to the site's mapping:

```scala
val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(unidocSettings: _*).
  settings(site.settings ++ ghpages.settings: _*).
  settings(
    name := "foo",
    site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "latest/api"),
    git.remoteRepo := "git@github.com:user/foo.git"
  ).
  aggregate(library, app)
```

Here's how to preview and publish it:

```
foo> preview-site
foo> ghpages-push-site
```

how to unify javadoc
--------------------

1. Add `genjavadocSettings` to child projects' settings.
2. Add `javaUnidocSettings` to the root project's settings.

```scala
val library = (project in file("library")).
  settings(commonSettings: _*).
  settings(genjavadocSettings: _*).
  settings(
    name := "foo-library"
  )

val app = (project in file("app")).
  settings(commonSettings: _*).
  settings(genjavadocSettings: _*).
  settings(
    name := "foo-app"
  ).
  dependsOn(library)

val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(javaUnidocSettings: _*).
  settings(
    name := "foo"
  ).
  aggregate(library, app)
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

1. Add `genjavadocExtraSettings` to the child projects.

This will substitute the `packageDoc in Compile` with `packageDoc in Genjavadoc` to use the enhanced Javadoc.

how to unify both Scaladoc and Javadoc
--------------------------------------

1. Add `genjavadocSettings` (or `genjavadocExtraSettings`) to child projects' settings.
2. Add `scalaJavaUnidocSettings` to the root project's settings.

This combines both Scala unidoc settings and Java unidoc settings. Run `unidoc` from the root project to execute both.

credits
-------

The original implementation of `unidoc` task was written by Peter Vlugter ([@pvlugter](https://github.com/pvlugter)) for Akka project. I took [Unidoc.scala](https://github.com/akka/akka/blob/05ba6df5acf48eaf447b5898787e63badbe02cf9/project/Unidoc.scala) in akka/akka@ed40dff7d7353c5cb15b7408ec049116081cb1fc and refactored it. sbt-unidoc is Open Source and available under the Apache 2 License.

  [genjavadoc]: https://github.com/typesafehub/genjavadoc
