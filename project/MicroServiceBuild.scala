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
  import play.PlayImport._
  import play.core.PlayVersion

  val compile = Seq(
    "uk.gov.hmrc" %% "play-reactivemongo" % "4.8.0",
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % "4.4.0",
    "uk.gov.hmrc" %% "play-authorisation" % "3.4.0",
    "uk.gov.hmrc" %% "play-config" % "2.1.0",
    "uk.gov.hmrc" %% "play-url-binders" % "1.1.0",
    "uk.gov.hmrc" %% "play-scheduling" % "3.0.0",
    "uk.gov.hmrc" %% "play-health" % "1.1.0",
    "uk.gov.hmrc" %% "play-json-logger" % "2.1.1",
    "uk.gov.hmrc" %% "http-verbs" % "5.0.0",
    "uk.gov.hmrc" %% "http-caching-client" % "5.6.0",
    "uk.gov.hmrc" %% "domain" % "3.7.0",
    "uk.gov.hmrc" %% "play-graphite" % "2.0.0",
    "com.github.fge" % "json-schema-validator" % "2.2.6"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "reactivemongo-test" % "1.6.0" % scope,
        "uk.gov.hmrc" %% "hmrctest" % "1.9.0" % scope,
        "org.scalatest" %% "scalatest" % "2.2.2" % scope,
        "org.scalacheck" %% "scalacheck" % "1.12.2" % scope,
        "org.pegdown" % "pegdown" % "1.4.2" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "com.github.fge" % "json-schema-validator" % "2.2.6"
      )
    }.test
  }

  def apply() = compile ++ Test()
}
