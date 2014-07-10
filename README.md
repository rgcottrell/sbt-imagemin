sbt-imagemin
=============
[![Build Status](https://travis-ci.org/rgcottrell/sbt-imagemin.svg?branch=master)](https://travis-ci.org/rgcottrell/sbt-imagemin)

[sbt-web](https://github.com/sbt/sbt-web) plugin that optimizes images using [imagemin](https://github.com/kevva/imagemin) on the asset pipeline.

To use the latest version from GitHub, add the following to the `project/plugins.sbt` of your project:

```scala
addSbtPlugin("com.slidingautonomy.sbt" % "sbt-imagemin" % "1.0.0")
```

Add the Sonatype releases resolver:

```scala
resolvers += Resolver.sonatypeRepo("releases")
```

Your project's build file also needs to enable sbt-web plugins. For example with build.sbt:

```scala
lazy val root = (project in file(".")).enablePlugins(SbtWeb)
```

As with all sbt-web asset pipeline plugins, you must declare their order of execution. For example:

```scala
pipelineStages := Seq(imagemin)
```

## Configuration

The plugin will optimize JPEG, GIF, PNG and SVG images for display on the web with a minimum amount of configuration.
The default options are:

```scala
ImageMinKeys.progressive       := true  // lossless conversion to progressive JPEG images 
ImageMinKeys.interlaced        := true  // interlaced GIF images
ImageMinKeys.optimizationLevel := 3     // optimization level 0 - 7 for PNG images
```

## File Filters

By default, the plugin scans for any assets ending in `jpeg`, `jpg`, `gif`, `png`, or `svg` and creates optimized
versions of these files. The files to be processed can be filtered using the includeFilter and excludeFilter
settings. For example, to optimize only PNG images:

```scala
includeFilter in imagemin := "*.png"
```

## Prerequisites

The plugin requires that your project have the imagemin Node module installed. The easiest way to do this is to
include a package.json file at the root of your project:

```json
{
  "dependencies": {
    "imagemin": "^0.4.6"
  }
}
```