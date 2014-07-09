package com.slidingautonomy.sbt.imagemin

import sbt._
import sbt.Keys._
import com.typesafe.sbt.jse.{SbtJsEngine, SbtJsTask}
import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.web.pipeline.Pipeline
import spray.json._

object Import {

  val imagemin = TaskKey[Pipeline.Stage]("imagemin", "Minify images on the asset pipeline.")

  object ImageMinKeys {
    val appDir = SettingKey[File]("imagemin-app-dir", "The top level directory that contains your app image files.")
    val buildDir = SettingKey[File]("imagemin-build-dir", "The target directory for the optimized image files.")
    val interlaced = SettingKey[Boolean]("imagemin-interlaced", "Enables interlacing of GIF images for progressive rendereing.")
    val optimizationLevel = SettingKey[Int]("imagemin-optimization-level", "Optimization level for PNG images, between 0 and 7.")
    val progressive = SettingKey[Boolean]("imagemin-progressive", "Enables lossless conversion of JPEG images to progressive.")
  }

}

object SbtImageMin extends AutoPlugin {

  override def requires = SbtJsTask

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtWeb.autoImport._
  import WebKeys._
  import SbtJsEngine.autoImport.JsEngineKeys._
  import SbtJsTask.autoImport.JsTaskKeys._
  import autoImport._
  import ImageMinKeys._

  override def projectSettings = Seq(
    appDir := (resourceManaged in imagemin).value / "appdir",
    buildDir := (resourceManaged in imagemin).value / "build",
    excludeFilter in imagemin := HiddenFileFilter,
    imagemin := runMinifier.dependsOn(nodeModules in Assets).value,
    includeFilter in imagemin := GlobFilter("*.jpg") | GlobFilter("*.jpeg") | GlobFilter("*.png") | GlobFilter("*.gif") | GlobFilter("*.svg"),
    interlaced := true,
    optimizationLevel := 3,
    progressive := true,
    resourceManaged in imagemin := webTarget.value / imagemin.key.label
  )

  private def runMinifier: Def.Initialize[Task[Pipeline.Stage]] = Def.task {
    mappings =>

      val include = (includeFilter in imagemin).value
      val exclude = (excludeFilter in imagemin).value
      val preMappings = mappings.filter(f => !f._1.isDirectory && include.accept(f._1) && !exclude.accept(f._1))
      SbtWeb.syncMappings(
        streams.value.cacheDirectory,
        preMappings,
        appDir.value
      )

      val cacheDirectory = streams.value.cacheDirectory / imagemin.key.label
      val runUpdate = FileFunction.cached(cacheDirectory, FilesInfo.hash) {
        inputFiles =>
          streams.value.log("Minifying images with imagemin")

          val sourceFileMappings = JsArray(inputFiles.filter(_.isFile).map { f =>
            val relativePath = IO.relativize(appDir.value, f).get
            JsArray(JsString(f.getPath), JsString(relativePath))
          }.toList).toString()

          val targetPath = buildDir.value.getPath

          val jsOptions = JsObject(
            "interlaced" -> JsBoolean(interlaced.value),
            "optimizationLevel" -> JsNumber(optimizationLevel.value),
            "progressive" -> JsBoolean(progressive.value)
          ).toString()

          val shellFile = SbtWeb.copyResourceTo(
            (resourceManaged in imagemin).value,
            getClass.getClassLoader.getResource("imagemin-shell.js"),
            streams.value.cacheDirectory / "copy-resource"
          )

          SbtJsTask.executeJs(
            state.value,
            (engineType in imagemin).value,
            (command in imagemin).value,
            (nodeModuleDirectories in Plugin).value.map(_.getPath),
            shellFile,
            Seq(sourceFileMappings, targetPath, jsOptions),
            (timeoutPerSource in imagemin).value * preMappings.size
          )

          buildDir.value.***.get.toSet
      }

      val postMappings = runUpdate(appDir.value.***.get.toSet).filter(_.isFile).pair(relativeTo(buildDir.value))
      (mappings.toSet -- preMappings ++ postMappings).toSeq
  }

}
