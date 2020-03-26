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

package controllers.application

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.ProcessingReport
import config.ApplicationGlobal
import connectors._
import connectors.securestorage.SecureStorage
import constants.Constants
import controllers.ControllerComponentsHelper
import json.JsonValidator
import metrics.MicroserviceMetrics
import models.application.ProbateDetails.probateDetailsReads
import models.application.basicElements.BasicEstateElement
import models.application.exemptions.{Charity, QualifyingBody}
import models.application.gifts.PreviousYearsGifts
import models.application.{ApplicationDetails, ProbateDetails}
import models.enums.Api
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.Mockito.{atMost => expected}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json._
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import services.AuditService
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.test.UnitSpec
import utils._
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{BadRequestException, GatewayTimeoutException, HeaderCarrier, HttpResponse, NotFoundException, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

class ApplicationControllerTest extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ControllerComponentsHelper {

  implicit val headerCarrier: FakeHeaders = FakeHeaders()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val hc: HeaderCarrier = new HeaderCarrier

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val errorHttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, None, Map(), None)
  val successHttpResponseForProbateDetails = HttpResponse(OK, Some(Json.parse(TestHelper.JsSampleProbateDetails)), Map())
  val noProbateDetailsHttpResponse = HttpResponse(NO_CONTENT, None, Map(), None)

  val mockDesConnector: IhtConnector = mock[IhtConnector]
  val mockJsonValidator: JsonValidator = mock[JsonValidator]
  val mockRegistrationHelper: RegistrationHelper = mock[RegistrationHelper]
  val mockSecureStorage: SecureStorage = mock[SecureStorage]
  var mockAuditService: AuditService = mock[AuditService]
  val mockMetrics: MicroserviceMetrics = mock[MicroserviceMetrics]
  val mockProcessingReport: ProcessingReport = mock[ProcessingReport]
  val mockAppGlobal: ApplicationGlobal = mock[ApplicationGlobal]
  val mockControllerComponents: ControllerComponents = mock[ControllerComponents]

  override def beforeEach(): Unit = {
    reset(mockDesConnector)
    reset(mockJsonValidator)
    reset(mockRegistrationHelper)
    reset(mockSecureStorage)
    reset(mockAuditService)
    reset(mockDesConnector)
    reset(mockProcessingReport)
    reset(mockMetrics)
    reset(mockAppGlobal)
    reset(mockControllerComponents)
    super.beforeEach()
  }

  class TestApplicationController extends BackendController(mockControllerComponents) with ApplicationController {
    override val ihtConnector: IhtConnector = mockDesConnector
    override val jsonValidator: JsonValidator = JsonValidator
    override lazy val secureStorage: SecureStorage = mockSecureStorage
    override val registrationHelper: RegistrationHelper = mockRegistrationHelper
    override val metrics: MicroserviceMetrics = mockMetrics
    override def auditService: AuditService = mockAuditService
    override val appGlobal: ApplicationGlobal = mockAppGlobal
  }

  def applicationController: ApplicationController = new TestApplicationController

  def applicationControllerMockedValidator: ApplicationController = {
    when(mockControllerComponents.actionBuilder)
      .thenReturn(testActionBuilder)

    new TestApplicationController {
      override val jsonValidator: JsonValidator = mockJsonValidator
    }
  }

  private def buildApplicationDetails = {
    val charity = Charity(
      id = Some("1"),
      name = Some("A Charity"),
      number = Some("234"),
      totalValue = Some(44.45)
    )

    val qualifyingBodies = QualifyingBody(
      id = Some("1"),
      name = Some("Qualifying Body"),
      totalValue = Some(12345)
    )

    ApplicationDetails(allAssets = None,
      propertyList = Nil,
      allLiabilities = None,
      allExemptions = None,
      charities = Seq(charity),
      qualifyingBodies = Seq(qualifyingBodies),
      widowCheck = None,
      increaseIhtThreshold = None,
      status = Constants.AppStatusInProgress,
      kickoutReason = None,
      ihtRef = Some("Abc123")
    )
  }

  val acknowledgementReference = AcknowledgementRefGenerator.getUUID

  class Setup {
    val rd = CommonBuilder.buildRegistrationDetailsDODandDeceasedDetails
    when(mockRegistrationHelper.getRegistrationDetails(any(), any()))
      .thenReturn(Some(rd))

    when(mockProcessingReport.isSuccess()).thenReturn(true)
    when(mockProcessingReport.iterator()).thenReturn(null)
  }

  val successHttpResponseForIhtReturn = HttpResponse(OK, Some(Json.parse(AcknowledgementRefGenerator.replacePlaceholderAckRefWithDefault(
    """
    {
    "acknowledgmentReference" : "<ACKREF>",
    "submitter" : {
    "submitterRole" : "Lead Executor"
    },
    "deceased" : { },
    "freeEstate" : {
    "estateAssets" : [ {
    "assetCode" : "0016",
    "assetDescription" : "Deceased's residence",
    "assetID" : null,
    "assetTotalValue" : 100000.00,
    "tenure" : "Freehold",
    "tenancyType" : "Vacant Possession",
    "yearsLeftOnLease" : 0,
    "yearsLeftOntenancyAgreement" : 0
    }, {
    "assetCode" : "9001",
    "assetDescription" : "Rolled up bank and building society accounts",
    "assetID" : null,
    "assetTotalValue" : 7500.00,
    "howheld" : "Standard"
    } ]
    },
    "declaration" : {
    "reasonForBeingBelowLimit" : "Excepted Estate",
    "declarationAccepted" : true,
    "coExecutorsAccepted" : true,
    "declarationDate" : "2015-08-24"
    }
    }
  """
  ))), Map())

  "Application Controller" must {

    "return JSON for application details on valid IHT Reference" in new Setup {
      when(mockSecureStorage.get(any(), any())(any())).thenReturn(Some(Json.toJson(new ApplicationDetails)))
      when(mockControllerComponents.actionBuilder).thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)

      val result = applicationController.get("", "IHT123", acknowledgementReference)(request)
      status(result) should be(OK)
    }

    "return empty application details with not started status" in new Setup {
      when(mockSecureStorage.get(any(), any())(any())).thenReturn(None)
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)

      val result = applicationController.get("", "IHT123", acknowledgementReference)(request)
      status(result) should be(OK)
      contentAsString(result) should include(Constants.AppStatusNotStarted)
    }

    "call persistence connector and return success on save" in new Setup {

      import scala.concurrent.ExecutionContext.Implicits.global

      doNothing().when(mockSecureStorage).update("chicken", acknowledgementReference, Json.toJson(new ApplicationDetails))
      when(mockSecureStorage.get(any(), any())(any())).thenReturn(None)
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockAuditService.sendEvent(any(), any[JsValue](), any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val applicationDetails = Json.toJson(buildApplicationDetails)

      val result = applicationController.save("", acknowledgementReference)(request.withBody(applicationDetails))

      status(result) should be(OK)
    }

    val expectedIhtReference = Some("IHT123")

    "call the audit service on save of single value" in new Setup {
      implicit val headnapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
      implicit val exenapper = ArgumentCaptor.forClass(classOf[ExecutionContext])

      val adBefore = CommonBuilder.buildApplicationDetailsAllFields.copy(ihtRef = expectedIhtReference)
      val moneyOwedAfter = BasicEstateElement(Some(BigDecimal(50)))
      val optionAllAssetsAfter = adBefore.allAssets.map(_ copy (moneyOwed = Some(moneyOwedAfter)))
      val adAfter = adBefore.copy(allAssets = optionAllAssetsAfter)

      when(mockSecureStorage.get(any(), any())(any())).thenReturn(Some(Json.toJson(adBefore)))
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockAuditService.sendEvent(any(), any[JsValue](), any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val result = applicationController.save("IHT123", acknowledgementReference)(request.withBody(Json.toJson(adAfter)))

      val eventCaptorForString = ArgumentCaptor.forClass(classOf[String])
      val eventCaptorForMap = ArgumentCaptor.forClass(classOf[Map[String, String]])

      verify(mockAuditService).sendEvent(eventCaptorForString.capture, eventCaptorForMap.capture, any())(headnapper.capture, any())
      eventCaptorForString.getValue shouldBe Constants.AuditTypeMonetaryValueChange
      eventCaptorForMap.getValue shouldBe Map(
        Constants.AuditTypeIHTReference -> expectedIhtReference.getOrElse(""),
        Constants.AuditTypeMoneyOwed + Constants.AuditTypePreviousValue -> "15",
        Constants.AuditTypeMoneyOwed + Constants.AuditTypeNewValue -> "50")
    }

    "call the audit service on save of multiple values" in new Setup {
      implicit val headnapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
      implicit val exenapper = ArgumentCaptor.forClass(classOf[ExecutionContext])
      val beforeGiftsList = CommonBuilder.buildGiftsList
      val adBefore = CommonBuilder.buildApplicationDetailsAllFields.copy(ihtRef = expectedIhtReference,
        giftsList = Some(beforeGiftsList))
      val optionAllGifts = Seq(
        PreviousYearsGifts(
          yearId = Some("1"),
          value = Some(BigDecimal(1000)),
          exemptions = Some(BigDecimal(0)),
          startDate = Some("6 April 2004"),
          endDate = Some("5 April 2005")
        ),
        PreviousYearsGifts(
          yearId = Some("2"),
          value = Some(BigDecimal(2000)),
          exemptions = Some(BigDecimal(200)),
          startDate = Some("6 April 2005"),
          endDate = Some("5 April 2006")
        ),
        PreviousYearsGifts(
          yearId = Some("3"),
          value = Some(BigDecimal(2000)),
          exemptions = Some(BigDecimal(0)),
          startDate = Some("6 April 2006"),
          endDate = Some("5 April 2007")
        ),
        PreviousYearsGifts(
          yearId = Some("4"),
          value = Some(BigDecimal(4000)),
          exemptions = Some(BigDecimal(10)),
          startDate = Some("6 April 2007"),
          endDate = Some("5 April 2008")
        ),
        PreviousYearsGifts(
          yearId = Some("5"),
          value = Some(BigDecimal(5000)),
          exemptions = Some(BigDecimal(0)),
          startDate = Some("6 April 2008"),
          endDate = Some("5 April 2009")
        ),
        PreviousYearsGifts(
          yearId = Some("6"),
          value = Some(BigDecimal(6000)),
          exemptions = Some(BigDecimal(0)),
          startDate = Some("6 April 2009"),
          endDate = Some("5 April 2010")
        ),
        PreviousYearsGifts(
          yearId = Some("7"),
          value = Some(BigDecimal(8000)),
          exemptions = Some(BigDecimal(0)),
          startDate = Some("6 April 2010"),
          endDate = Some("5 April 2011")
        ))
      val adAfter = adBefore.copy(giftsList = Some(optionAllGifts))

      when(mockSecureStorage.get(any(), any())(any())).thenReturn(Some(Json.toJson(adBefore)))
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockAuditService.sendEvent(any(), any[JsValue](), any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val result = applicationController.save("IHT123", acknowledgementReference)(request.withBody(Json.toJson(adAfter)))

      val eventCaptorForString = ArgumentCaptor.forClass(classOf[String])
      val eventCaptorForMap = ArgumentCaptor.forClass(classOf[Map[String, String]])

      verify(mockAuditService).sendEvent(eventCaptorForString.capture, eventCaptorForMap.capture, any())(headnapper.capture, any())
      eventCaptorForString.getValue shouldBe Constants.AuditTypeMonetaryValueChange
      eventCaptorForMap.getValue shouldBe Map(
        Constants.AuditTypeIHTReference -> expectedIhtReference.getOrElse(""),
        Constants.AuditTypeGifts + Constants.AuditTypePreviousValue -> "27800",
        Constants.AuditTypeGifts + Constants.AuditTypeNewValue -> "27790")
    }

    "return internal server error when valid front-end JSON submitted but failure response returned from DES with a reason" in new Setup {
      val incorrectIhtFailureJson = Json.parse("""{"failureResponse":{"reason":"IHT Case not found"}}""")
      val errorHttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Some(incorrectIhtFailureJson), Map(), None)
      val jsonAD = Json.toJson(CommonBuilder.buildApplicationDetailsAllFields)

      when(mockJsonValidator.validate(any(), any())).thenReturn(mockProcessingReport)
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockDesConnector.submitApplication(any(), any(), any())(any())).thenReturn(Future.successful(errorHttpResponse))
      doNothing().when(mockMetrics).incrementFailedCounter(any())

      val result = applicationControllerMockedValidator.submit("", "")(request.withBody(jsonAD))

      status(result) should be(INTERNAL_SERVER_ERROR)
      //contentAsString(result) should include("Failure response received:")
    }

    "return internal server error when valid front-end JSON submitted but failure response returned from DES without a reason" in new Setup {
      val incorrectIhtFailureJson = Json.parse("""{"failureResponse":{"bla":"bla"}}""")
      val errorHttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Some(incorrectIhtFailureJson), Map(), None)
      val jsonAD = Json.toJson(CommonBuilder.buildApplicationDetailsAllFields)

      when(mockJsonValidator.validate(any(), any())).thenReturn(mockProcessingReport)
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockDesConnector.submitApplication(any(), any(), any())(any()))
        .thenReturn(Future.successful(errorHttpResponse))
      doNothing().when(mockMetrics).incrementFailedCounter(any())

      val result = applicationControllerMockedValidator.submit("", "")(request.withBody(jsonAD))
      status(result) should be(INTERNAL_SERVER_ERROR)
      //contentAsString(result) should include("Failure response received but no reason found")
    }

    "return internal server error when valid front-end JSON submitted but neither failure nor success response returned from DES" in new Setup {
      val incorrectIhtFailureJson = Json.parse("""{"bla":{"bla":"bla"}}""")
      val errorHttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Some(incorrectIhtFailureJson), Map(), None)
      val jsonAD = Json.toJson(CommonBuilder.buildApplicationDetailsAllFields)


      when(mockJsonValidator.validate(any(), any())).thenReturn(mockProcessingReport)
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockDesConnector.submitApplication(any(), any(), any())(any()))
        .thenReturn(Future.successful(errorHttpResponse))
      doNothing().when(mockMetrics).incrementFailedCounter(any())

      val result = applicationControllerMockedValidator.submit("", "")(request.withBody(jsonAD))
      status(result) should be(INTERNAL_SERVER_ERROR)
    }

    "return success response when valid front end JSON submitted" in new Setup {
      val correctIhtSuccessJson = Json.parse(
        """{"processingDate":"2001-12-17T09:30:47Z","returnId":"1234567890","versionNumber":"1234567890"}""")
      val correctHttpResponse = HttpResponse(OK, Some(correctIhtSuccessJson), Map(), None)
      val jsonAD = Json.toJson(CommonBuilder.buildApplicationDetailsAllFields.copy(ihtRef = Some("12345678")))

      when(mockAuditService.sendEvent(any(), any[JsValue](), any())(any(), any()))
        .thenReturn(Future.successful(Success))
      when(mockAuditService.sendEvent(any(), any(classOf[Map[String, String]]), any())(any(), any()))
        .thenReturn(Future.successful(Success))
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)

      when(mockDesConnector.submitApplication(any(), any(), any())(any())).thenReturn(Future.successful(correctHttpResponse))
      when(mockAuditService.sendEvent(any(), any[JsValue](), any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val result = applicationController.submit("", "")(request.withBody(jsonAD))
      status(result) should be(OK)
      verify(mockMetrics, times(1)).incrementSuccessCounter(Api.SUB_APPLICATION)
    }

    "return forbidden response when nino found is not that of the lead executor" in {
      val rd = CommonBuilder.buildRegistrationDetailsCoExecs
      when(mockRegistrationHelper.getRegistrationDetails(any(), any()))
        .thenReturn(Some(rd))

      when(mockJsonValidator.validate(any(), any())).thenReturn(mockProcessingReport)
      when(mockProcessingReport.isSuccess()).thenReturn(true)
      when(mockProcessingReport.iterator()).thenReturn(null)

      val correctIhtSuccessJson = Json.parse(
        """{"processingDate":"2001-12-17T09:30:47Z","returnId":"1234567890","versionNumber":"1234567890"}""")
      val correctHttpResponse = HttpResponse(OK, Some(correctIhtSuccessJson), Map(), None)
      val jsonAD = Json.toJson(CommonBuilder.buildApplicationDetailsAllFields.copy(ihtRef = Some("12345678")))

      when(mockAuditService.sendEvent(any(), any[JsValue](), any())(any(), any()))
        .thenReturn(Future.successful(Success))
      when(mockAuditService.sendEvent(any(), any(classOf[Map[String, String]]), any())(any(), any()))
        .thenReturn(Future.successful(Success))
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)

      val result = applicationController.submit("","AA019283A")(request.withBody(jsonAD))
      status(result) should be (FORBIDDEN)
      verify(mockDesConnector, times(0)).submitApplication(any(), any(), any())(any())
    }

    "send an audit event containing the final estate value on a successful submission" in new Setup {
      implicit val headnapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
      implicit val exenapper = ArgumentCaptor.forClass(classOf[ExecutionContext])

      val expectedIhtReference = Some("12345678")

      val correctIhtSuccessJson = Json.parse(
        """{"processingDate":"2001-12-17T09:30:47Z","returnId":"1234567890","versionNumber":"1234567890"}""")
      val correctHttpResponse = HttpResponse(OK, Some(correctIhtSuccessJson), Map(), None)
      val jsonAD = Json.toJson(CommonBuilder.buildApplicationDetailsAllFields.copy(ihtRef = expectedIhtReference))

      val eventCaptorForString = ArgumentCaptor.forClass(classOf[String])
      val eventCaptorForMap = ArgumentCaptor.forClass(classOf[Map[String, String]])

      when(mockAuditService.sendEvent(any(), any(classOf[Map[String, String]]), any())(any(), any()))
        .thenReturn(Future.successful(Success))
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockAuditService.sendEvent(any(), any[JsValue](), any())(any(), any()))
        .thenReturn(Future.successful(Success))
      when(mockDesConnector.submitApplication(any(), any(), any())(any())).thenReturn(Future.successful(correctHttpResponse))
      when(mockAuditService.sendEvent(any(), any[JsValue](), any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val result = applicationController.submit(expectedIhtReference.getOrElse(""), "")(request.withBody(jsonAD))

      status(result) shouldBe OK

      verify(mockAuditService).sendEvent(eventCaptorForString.capture, eventCaptorForMap.capture, any())(headnapper.capture, any())
      eventCaptorForString.getValue shouldBe Constants.AuditTypeFinalEstateValue
      eventCaptorForMap.getValue shouldBe Map(
        Constants.AuditTypeIHTReference -> expectedIhtReference.getOrElse(""),
        Constants.AuditTypeValue -> "28090")
    }

    "return no content response when valid front end JSON submitted to real time risking" in new Setup {

      val ihtNoRulesFiredResponseJson = Json.parse(JsonLoader
        .fromResource("/json/des/risking/RiskOutcomeNoRulesFired.json").toString)

      val correctHttpResponse = HttpResponse(OK, Some(ihtNoRulesFiredResponseJson), Map(), None)


      when(mockRegistrationHelper.getRegistrationDetails(any(), any()))
        .thenReturn(Some(CommonBuilder.buildRegistrationDetailsDODandDeceasedDetails))
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockDesConnector.submitRealtimeRisking(any(), any(), any()))
        .thenReturn(Future.successful(correctHttpResponse))
      val result = applicationController.getRealtimeRiskingMessage("", "")(request)
      status(result) should be(NO_CONTENT)
    }

    "return no content response when valid front end JSON submitted to real time risking but rules fired in risking" in new Setup {

      val ihtRulesFiredResponseJson = Json.parse(JsonLoader
        .fromResource("/json/des/risking/RiskOutcomeRulesFiredNotInclRule2.json").toString)

      val correctHttpResponse = HttpResponse(OK, Some(ihtRulesFiredResponseJson), Map(), None)

      when(mockRegistrationHelper.getRegistrationDetails(any(), any()))
        .thenReturn(Some(CommonBuilder.buildRegistrationDetailsDODandDeceasedDetails))
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockDesConnector.submitRealtimeRisking(any(), any(), any()))
        .thenReturn(Future.successful(correctHttpResponse))
      val result = applicationController.getRealtimeRiskingMessage("", "")(request)
      status(result) should be(NO_CONTENT)
    }

    "return success response when valid front end JSON submitted to real time risking but rules fired in risking including rule 2 (money)" in new Setup {

      val ihtRulesFiredResponseJson = Json.parse(JsonLoader
        .fromResource("/json/des/risking/RiskOutcomeRulesFiredInclRule2.json").toString)

      val correctHttpResponse = HttpResponse(OK, Some(ihtRulesFiredResponseJson), Map(), None)
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockRegistrationHelper.getRegistrationDetails(any(), any()))
        .thenReturn(Some(CommonBuilder.buildRegistrationDetailsDODandDeceasedDetails))
      when(mockDesConnector.submitRealtimeRisking(any(), any(), any()))
        .thenReturn(Future.successful(correctHttpResponse))

      val result = applicationController.getRealtimeRiskingMessage("", "")(request)

      verify(mockMetrics, expected(1)).incrementSuccessCounter(Api.SUB_REAL_TIME_RISKING)
      status(result) should be(OK)
    }

    "return OK when clearance successfully requested" in new Setup {
      val correctIhtSuccessJson = Json.toJson("""{ "clearanceStatus": { "status": "Clearance Granted", "statusDate": "2015-04-29" }, "caseStatus":"Clearance Granted" }""")
      val correctHttpResponse = HttpResponse(OK, Some(correctIhtSuccessJson), Map())

      when(mockDesConnector.requestClearance(any(), any(), any()))
        .thenReturn(Future.successful(correctHttpResponse))
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)

      val result = applicationController.requestClearance("", "")(request)

      status(result) should be(OK)
      verify(mockMetrics, expected(1)).incrementSuccessCounter(Api.SUB_REQUEST_CLEARANCE)
    }

    "correct Probate Details values are returned after parsing JS response " in new Setup {
      val json = Json.parse(TestHelper.JsSampleProbateDetails)
      val jsonValueAfterParsing: ProbateDetails = Json.fromJson((json \ "probateTotals").get)(probateDetailsReads)
        .getOrElse(throw new RuntimeException("Probate Details response not parsed properly"))

      assert(jsonValueAfterParsing.probateReference == "12345678A01-123", "Probate Reference is AAA111222")
      assert(jsonValueAfterParsing.grossEstateforIHTPurposes == 123456.78, "grossEstateforIHTPurposes is 123456.78")

    }

    "Respond appropriately to a failure response while fetching Probate Details" in new Setup {
      when(mockDesConnector.getProbateDetails(any(), any(), any())).thenReturn((Future(errorHttpResponse)))
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)

      val result = applicationController.getProbateDetails("", "", "")(request)
      status(result) should be(INTERNAL_SERVER_ERROR)
    }

    "Respond with no content if a Probate Detail is not available" in new Setup {
      when(mockDesConnector.getProbateDetails(any(), any(), any())).thenReturn((Future(noProbateDetailsHttpResponse)))
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)

      val result = applicationController.getProbateDetails("", "", "")(request)
      status(result) should be(NO_CONTENT)
    }

    "Respond with OK on successful return of Probate Detail" in new Setup {
      when(mockDesConnector.getProbateDetails(any(), any(), any())).thenReturn(Future(successHttpResponseForProbateDetails))
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)

      val result = applicationController.getProbateDetails("", "", "")(request)
      status(result) should be(OK)
      verify(mockMetrics, expected(1)).incrementSuccessCounter(Api.GET_PROBATE_DETAILS)
    }

    "Respond appropriately to a failure response while fetching IHT return details" in new Setup {
      when(mockDesConnector.getSubmittedApplicationDetails(any(), any(), any())).thenReturn((Future(errorHttpResponse)))
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)

      val result = applicationController.getSubmittedApplicationDetails("", "", "")(request)
      status(result) should be(INTERNAL_SERVER_ERROR)
    }

    "Respond appropriately to a schema validation failure while fetching IHT return details" in new Setup {
      val incorrectIhtReturnJson = Json.toJson("""{ "SomeRubbish":"Not an IHT return" }""")
      val incorrectHttpResponse = HttpResponse(OK, Some(incorrectIhtReturnJson), Map())
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockDesConnector.getSubmittedApplicationDetails(any(), any(), any())).thenReturn((Future(incorrectHttpResponse)))

      val result = applicationController.getSubmittedApplicationDetails("", "", "")(request)
      status(result) should be(INTERNAL_SERVER_ERROR)
    }

    "Respond appropriately when the IHT return details are not found" in new Setup {
      val noIhtReturnHttpResponse = HttpResponse(NOT_FOUND, None, Map(), None)

      when(mockDesConnector.getSubmittedApplicationDetails(any(), any(), any())).thenReturn((Future(noIhtReturnHttpResponse)))
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)

      val result = applicationController.getSubmittedApplicationDetails("", "", "")(request)
      status(result) should be(INTERNAL_SERVER_ERROR)
    }

    "Respond with OK on successful return of IHT return details" in new Setup {
      when(mockDesConnector.getSubmittedApplicationDetails(any(), any(), any())).thenReturn(Future(successHttpResponseForIhtReturn))
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)

      val result = applicationController.getSubmittedApplicationDetails("", "", "")(request)
      status(result) should be(OK)
      verify(mockMetrics, expected(1)).incrementSuccessCounter(Api.GET_APPLICATION_DETAILS)
    }

    def genericExceptionHandlingTestSetup = {
      val incorrectIhtFailureJson = Json.parse("""{"failureResponse":{"reason":"IHT Case not found"}}""")
      val errorHttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Some(incorrectIhtFailureJson), Map(), None)
      val jsonAD = Json.toJson(CommonBuilder.buildApplicationDetailsAllFields)

      when(mockJsonValidator.validate(any(), any())).thenReturn(mockProcessingReport)
      when(mockControllerComponents.actionBuilder)
        .thenReturn(testActionBuilder)
      when(mockControllerComponents.parsers)
        .thenReturn(stubPlayBodyParsers)
      when(mockDesConnector.submitApplication(any(), any(), any())(any()))
        .thenReturn(Future.successful(errorHttpResponse))
      doNothing().when(mockMetrics).incrementFailedCounter(any())
      jsonAD
    }

    "handle gateway exception when required" in new Setup {
      val jsonAD: JsValue = genericExceptionHandlingTestSetup

      when(mockDesConnector.submitApplication(any(), any(), any())(any()))
        .thenReturn(Future.failed(new GatewayTimeoutException("gateway-exception")))

      try {
        applicationControllerMockedValidator.submit("", "")(request.withBody(jsonAD))
        fail("Exception not thrown")
      } catch {
        case ex: Exception => // Do nothing
      }
    }

    "handle BadRequestException exception when required" in new Setup {
      val jsonAD: JsValue = genericExceptionHandlingTestSetup

      when(mockDesConnector.submitApplication(any(), any(), any())(any()))
        .thenReturn(Future.failed(new BadRequestException("bad-request-exception")))

      try {
        applicationControllerMockedValidator.submit("", "")(request.withBody(jsonAD))
        fail("Exception not thrown")
      } catch {
        case ex: Exception => // Do nothing
      }
    }

    "handle Upstream4xxResponse exception when required" in new Setup {
      val jsonAD: JsValue = genericExceptionHandlingTestSetup

      when(mockDesConnector.submitApplication(any(), any(), any())(any()))
        .thenReturn(Future.failed(new Upstream4xxResponse("upstream-exception", -1, -1)))

      try {
        applicationControllerMockedValidator.submit("", "")(request.withBody(jsonAD))
        fail("Exception not thrown")
      } catch {
        case ex: Exception => // Do nothing
      }
    }

    "handle Upstream5xxResponse exception when required" in new Setup {
      val jsonAD: JsValue = genericExceptionHandlingTestSetup

      when(mockDesConnector.submitApplication(any(), any(), any())(any()))
        .thenReturn(Future.failed(new Upstream5xxResponse("gateway-exception", -1, -1)))

      try {
        applicationControllerMockedValidator.submit("", "")(request.withBody(jsonAD))
        fail("Exception not thrown")
      } catch {
        case ex: Exception => // Do nothing
      }
    }

    "handle NotFoundException exception when required" in new Setup {
      val jsonAD: JsValue = genericExceptionHandlingTestSetup

      when(mockDesConnector.submitApplication(any(), any(), any())(any()))
        .thenReturn(Future.failed(new NotFoundException("notfound-exception")))

      try {
        applicationControllerMockedValidator.submit("", "")(request.withBody(jsonAD))
        fail("Exception not thrown")
      } catch {
        case ex: Exception => // Do nothing
      }
    }

    "handle Generic Exception exception when required" in new Setup {
      val jsonAD: JsValue = genericExceptionHandlingTestSetup

      when(mockDesConnector.submitApplication(any(), any(), any())(any()))
        .thenReturn(Future.failed(new RuntimeException("generic-exception")))

      try {
        applicationControllerMockedValidator.submit("", "")(request.withBody(jsonAD))
        fail("Exception not thrown")
      } catch {
        case ex: Exception => // Do nothing
      }
    }
  }
}
