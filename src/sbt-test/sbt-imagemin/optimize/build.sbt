import JsEngineKeys._

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

pipelineStages := Seq(imagemin)

val checkImageSizes = taskKey[Unit]("check that the images are optimized")

checkImageSizes := {
  def checkImage(name: String) {
    val original = IO.read(file(s"src/main/public/img/$name"))
    val optimized = IO.read(file(s"target/web/stage/img/$name"))
    if (original.size <= optimized.size) {
      sys.error(s"Expected image to be optimized: $name (original: ${original.size} => ${optimized.size}")
    }
  }
  checkImage("play.png")
  checkImage("play.svg")
}