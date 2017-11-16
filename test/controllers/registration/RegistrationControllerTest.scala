/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.registration

import connectors.IhtConnector
import metrics.Metrics
import models.enums._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.play.test.UnitSpec
import utils.CommonBuilder._
import utils.{AcknowledgementRefGenerator, FakeIhtApp, NinoBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, Upstream4xxResponse}

class RegistrationControllerTest extends UnitSpec with FakeIhtApp with MockitoSugar {

  val mockDesConnector: IhtConnector = mock[IhtConnector]

  def testRegistrationController = new RegistrationController {
    override val desConnector = mockDesConnector
  }

  "RegistrationController" must {
    import com.github.fge.jackson.JsonLoader
    implicit val headerCarrier = FakeHeaders()
    implicit val request = FakeRequest()
    implicit val hc = new HeaderCarrier

    // Mocked up data
    val correctIhtReferenceNoJs = Json.parse( """{"referenceNumber":"AAA111222"}""" )
    val invalidIhtReferenceNoJs = Json.parse( """{"bla":"bla"}""" )
    val ihtRegistrationDetails = Json.parse(
      AcknowledgementRefGenerator.replacePlaceholderAckRefWithDefault(
        NinoBuilder.replacePlaceholderNinoWithDefault(
          JsonLoader.fromResource("/json/validation/JsonTestValid.json").toString)))

    val correctHttpResponse = HttpResponse(OK,Some(correctIhtReferenceNoJs),Map(),None)
    val invalidHttpResponse = HttpResponse(OK,Some(invalidIhtReferenceNoJs),Map(),None)

    "respond with OK if HttpResponse is correct" in {
      when(mockDesConnector.submitRegistration(any(),any())).thenReturn(Future(correctHttpResponse))
      val result = testRegistrationController.submit(DefaultNino)(request.withBody(ihtRegistrationDetails))
      status(result) should be(OK)
    }

    "allow a submission to the connector and return reference number as text if HttpResponse is correct" in {
      when(mockDesConnector.submitRegistration(any(),any())).thenReturn(Future(correctHttpResponse))
      val result = testRegistrationController
        .submit(DefaultNino)(request.withBody(ihtRegistrationDetails))
      contentAsString(result) should be("AAA111222")
      assert(Metrics.successCounters(Api.SUB_REGISTRATION).getCount>0, "Success counter for Sub Registration Api is more than one")
    }

    "respond appropriately to a failure response" in {
      when(mockDesConnector.submitRegistration(any(),any())).thenReturn(Future(invalidHttpResponse))
      val result = testRegistrationController.submit(DefaultNino)(request.withBody(ihtRegistrationDetails))
      status(result) should be(INTERNAL_SERVER_ERROR)
    }

    "respond with ACCEPTED if 409 exception thrown by DES" in {
      when(mockDesConnector.submitRegistration(any(),any())).thenReturn(Future.failed(Upstream4xxResponse("", CONFLICT, CONFLICT)))
      val result = testRegistrationController.submit(DefaultNino)(request.withBody(ihtRegistrationDetails))
      status(result) should be(ACCEPTED)
    }

    "respond with Upstream4xxResponse if 404 exception thrown by DES" in {
      when(mockDesConnector.submitRegistration(any(),any())).thenReturn(Future.failed(Upstream4xxResponse("", NOT_FOUND, NOT_FOUND)))
      a[Upstream4xxResponse] shouldBe thrownBy {
        await(testRegistrationController.submit(DefaultNino)(request.withBody(ihtRegistrationDetails)))
      }
    }
  }
}
