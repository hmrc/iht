/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import connectors.IhtConnector
import controllers.ControllerComponentsHelper
import metrics.MicroserviceMetrics
import models.enums._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AuditService
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.play.test.UnitSpec
import utils.CommonBuilder._
import utils.{AcknowledgementRefGenerator, NinoBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationControllerTest extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ControllerComponentsHelper {

  val mockDesConnector: IhtConnector = mock[IhtConnector]
  val mockAuditService: AuditService = mock[AuditService]
  val mockMetrics: MicroserviceMetrics = mock[MicroserviceMetrics]
  val mockControllerComponents: ControllerComponents = mock[ControllerComponents]

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def testRegistrationController: RegistrationController = {
    when(mockAuditService.sendEvent(ArgumentMatchers.any(), ArgumentMatchers.any[JsValue](), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Success))

    class TestClass extends BackendController(mockControllerComponents) with RegistrationController {
      override val desConnector: IhtConnector = mockDesConnector
      override val auditService: AuditService = mockAuditService
      override val metrics: MicroserviceMetrics = mockMetrics
    }

    new TestClass
  }

  override def beforeEach(): Unit = {
    reset(mockDesConnector)
    reset(mockAuditService)
    reset(mockMetrics)
    reset(mockControllerComponents)
    super.beforeEach()
  }

  "RegistrationController" must {
    import com.github.fge.jackson.JsonLoader
    implicit val request = FakeRequest()

    // Mocked up data
    val correctIhtReferenceNoJs = Json.parse("""{"referenceNumber":"AAA111222"}""")
    val invalidIhtReferenceNoJs = Json.parse("""{"bla":"bla"}""")
    val ihtRegistrationDetails = Json.parse(
      AcknowledgementRefGenerator.replacePlaceholderAckRefWithDefault(
        NinoBuilder.replacePlaceholderNinoWithDefault(
          JsonLoader.fromResource("/json/validation/JsonTestValid.json").toString)))

    val correctHttpResponse = HttpResponse(OK, Some(correctIhtReferenceNoJs), Map(), None)
    val invalidHttpResponse = HttpResponse(OK, Some(invalidIhtReferenceNoJs), Map(), None)

    "respond with OK if HttpResponse is correct" in {
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockDesConnector.submitRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future(correctHttpResponse))

      val result = testRegistrationController.submit(DefaultNino)(request.withBody(ihtRegistrationDetails))
      status(result) should be(OK)
    }

    "allow a submission to the connector and return reference number as text if HttpResponse is correct" in {
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockDesConnector.submitRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future(correctHttpResponse))

      val result = testRegistrationController
        .submit(DefaultNino)(request.withBody(ihtRegistrationDetails))
      contentAsString(result) should be("AAA111222")
      verify(mockMetrics, times(1)).incrementSuccessCounter(Api.SUB_REGISTRATION)
    }

    "respond appropriately to a failure response" in {
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockDesConnector.submitRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future(invalidHttpResponse))

      val result = testRegistrationController.submit(DefaultNino)(request.withBody(ihtRegistrationDetails))
      status(result) should be(INTERNAL_SERVER_ERROR)
    }

    "respond with ACCEPTED if 409 exception thrown by DES" in {
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockDesConnector.submitRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.failed(Upstream4xxResponse("", CONFLICT, CONFLICT)))

      val result = testRegistrationController.submit(DefaultNino)(request.withBody(ihtRegistrationDetails))
      status(result) should be(ACCEPTED)
    }

    "respond with Upstream4xxResponse if 404 exception thrown by DES" in {
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockDesConnector.submitRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.failed(Upstream4xxResponse("", NOT_FOUND, NOT_FOUND)))

      a[Upstream4xxResponse] shouldBe thrownBy {
        await(testRegistrationController.submit(DefaultNino)(request.withBody(ihtRegistrationDetails)))
      }
    }

    "respond with timeout" in {
      setupBodyBuilders

      when(mockDesConnector.submitRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new GatewayTimeoutException("des_gateway_timeout")))

      try {
        await(testRegistrationController.submit(DefaultNino)(request.withBody(ihtRegistrationDetails)))
        fail("Exception not thrown")
      } catch {case _ :Throwable => }
    }

    "respond with bad request exception" in {
      setupBodyBuilders

      when(mockDesConnector.submitRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new BadRequestException("des_bad_request")))

      try {
        await(testRegistrationController.submit(DefaultNino)(request.withBody(ihtRegistrationDetails)))
        fail("Exception not thrown")
      } catch {case _ :Throwable => }
    }

    "respond with bad not found exception" in {
      setupBodyBuilders

      when(mockDesConnector.submitRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("des_not_found")))

      try {
        await(testRegistrationController.submit(DefaultNino)(request.withBody(ihtRegistrationDetails)))
        fail("Exception not thrown")
      } catch {case _ :Throwable=> }
    }

    "respond with general exception" in {
      setupBodyBuilders

      when(mockDesConnector.submitRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("des_general_exception")))

      try {
        await(testRegistrationController.submit(DefaultNino)(request.withBody(ihtRegistrationDetails)))
        fail("Exception not thrown")
      } catch {case _ :Throwable=> }
    }

    "respond with upstream 500" in {
      setupBodyBuilders

      when(mockDesConnector.submitRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new Upstream5xxResponse("des_gateway_timeout", 1, 1)))

      a[Upstream5xxResponse] shouldBe thrownBy {
        await(testRegistrationController.submit(DefaultNino)(request.withBody(ihtRegistrationDetails)))
      }
    }
  }

  private def setupBodyBuilders = {
    when(mockControllerComponents.actionBuilder)
      .thenReturn(testActionBuilder)
    when(mockControllerComponents.parsers)
      .thenReturn(stubPlayBodyParsers)
  }
}
