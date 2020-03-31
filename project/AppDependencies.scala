import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val jsonSchemaValidatorVersion = "2.2.6"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "simple-reactivemongo" % "7.26.0-play-26",
    "uk.gov.hmrc" %% "bootstrap-play-26" % "1.6.0",
    "uk.gov.hmrc" %% "http-caching-client" % "9.0.0-play-26",
    "uk.gov.hmrc" %% "domain" % "5.6.0-play-26",
    "com.github.fge" % "json-schema-validator" % jsonSchemaValidatorVersion,
    "com.typesafe.play" %% "play-json-joda" % "2.6.13"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  val reactiveMongoTestVersion = "4.19.0-play-26"
  val hmrcTestVersion = "3.9.0-play-26"
  val scalatestVersion = "3.0.8"
  val scalatestPlusVersion = "3.1.3"
  val pegdownVersion = "1.6.0"

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "reactivemongo-test" % reactiveMongoTestVersion % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalatestVersion % scope,
        "org.scalacheck" %% "scalacheck" % "1.14.3" % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "com.github.fge" % "json-schema-validator" % jsonSchemaValidatorVersion % scope,
        "org.mockito" % "mockito-core" % "3.3.3" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalatestPlusVersion % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val scope: String = "it"
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "reactivemongo-test" % reactiveMongoTestVersion % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalatestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalatestPlusVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "com.github.fge" % "json-schema-validator" % jsonSchemaValidatorVersion % scope,
        "org.mockito" % "mockito-all" % "1.10.19" % scope,
        "com.github.tomakehurst" % "wiremock-jre8" % "2.26.3" % "test,it"
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
