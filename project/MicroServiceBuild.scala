/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "iht"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  val compile = Seq(
    "org.reactivemongo" %% "reactivemongo" % "0.16.4" force(),
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25" % "4.9.0",
    "uk.gov.hmrc" %% "play-scheduling" % "6.0.0",
    "uk.gov.hmrc" %% "http-caching-client" % "8.1.0",
    "uk.gov.hmrc" %% "domain" % "5.6.0-play-25",
    "com.github.fge" % "json-schema-validator" % "2.2.6"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  val reactiveMongoTestVersion = "4.9.0-play-25"
  val hmrcTestVersion = "3.6.0-play-25"
  val jsonSchemaValidatorVersion = "2.2.6"

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "reactivemongo-test" % reactiveMongoTestVersion % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % "3.0.0" % scope,
        "org.scalacheck" %% "scalacheck" % "1.13.4" % scope,
        "org.pegdown" % "pegdown" % "1.6.0" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "com.github.fge" % "json-schema-validator" % jsonSchemaValidatorVersion % scope,
        "org.mockito" % "mockito-core" % "2.19.0" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {
      override lazy val scope: String = "it"
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "reactivemongo-test" % reactiveMongoTestVersion % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % "3.0.0" % scope,
        "org.pegdown" % "pegdown" % "1.4.2" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "com.github.fge" % "json-schema-validator" % jsonSchemaValidatorVersion % scope,
        "org.mockito" % "mockito-all" % "1.9.5" % scope,
        "com.github.tomakehurst" % "wiremock" % "2.9.0" % scope
      )
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()
}
