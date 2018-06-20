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

package util

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.StubMapping

trait WiremockHelper extends FakeConfig {

  val url = s"http://$wiremockHost:$wiremockPort"
  val wmConfig = wireMockConfig().port(wiremockPort)
  val wireMockServer = new WireMockServer(wmConfig)

  def startWiremock(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(wiremockHost, wiremockPort)
  }

  def stopWiremock(): Unit = wireMockServer.stop()

  def resetWiremock(): Unit = WireMock.reset()

  def stubGet(url: String, status: Integer, body: String): StubMapping = {
    removeStub(get(urlPathMatching(url)))
    stubFor(get(urlPathMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(body)
      )
    )
  }

  def stubPost(url: String, status: Integer, responseBody: String): StubMapping = {
    removeStub(post(urlMatching(url)))
    stubFor(post(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )
  }

  def stubPatch(url: String, status: Integer, responseBody: String): StubMapping = {
    stubFor(patch(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )
  }

  def stubPut(url: String, status: Integer, responseBody: String): StubMapping = {
    removeStub(put(urlMatching(url)))
    stubFor(put(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )
  }
}


trait FakeConfig {
  val wiremockPort = 11111
  val wiremockHost = "localhost"

  def fakeConfig(additionalConfig: Map[String, String] = Map.empty): Map[String, String] = Map(
    "microservice.services.iht.host" -> wiremockHost,
    "microservice.services.iht.port" -> wiremockPort.toString,
    "microservice.services.auth.host" -> wiremockHost,
    "microservice.services.auth.port" -> wiremockPort.toString,
    "auditing.enabled" -> "false",
    "auditing.traceRequests" -> "false",
    "auditing.consumer.baseUri.host" -> wiremockHost,
    "auditing.consumer.baseUri.port" -> wiremockPort.toString,
    "microservice.services.iht.des.authorization-key" -> "DESKEY",
    "microservice.services.iht.des.environment" -> "DES"
  ) ++ additionalConfig
}
