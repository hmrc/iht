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

package controllers

import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.ProcessingReport
import connectors._
import connectors.securestorage.SecureStorage
import constants.Constants
import json.JsonValidator
import metrics.Metrics
import models.ProbateDetails.probateDetailsReads
import models._
import models.enums.Api
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import services.AuditService
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ApplicationControllerTest extends UnitSpec with FakeIhtApp with MockitoSugar with BeforeAndAfter {

  implicit val headerCarrier = FakeHeaders()
  implicit val request = FakeRequest()
  implicit val hc = new HeaderCarrier
  val errorHttpResponse = HttpResponse(INTERNAL_SERVER_ERROR,None,Map(),None)
  val successHttpResponseForProbateDetails=HttpResponse(OK,Some(Json.parse(TestHelper.JsSampleProbateDetails)),Map())
  val noProbateDetailsHttpResponse = HttpResponse(NO_CONTENT,None,Map(),None)

  val mockDesConnector: IHTConnector = mock[IHTConnector]
  val mockJsonValidator: JsonValidator = mock[JsonValidator]
  val mockRegistrationHelper: RegistrationHelper = mock[RegistrationHelper]
  val mockSecureStorage : SecureStorage = mock[SecureStorage]
  var mockAuditService: AuditService = mock[AuditService]

   before {
     mockAuditService = mock[AuditService]
   }

  def applicationController = new ApplicationController {

    override val desConnector = mockDesConnector
    override val jsonValidator = JsonValidator
    override val registrationHelper = mockRegistrationHelper
    override lazy val secureStorage = mockSecureStorage
    override def metrics: Metrics = Metrics
    override def auditService = mockAuditService
  }

  def applicationControllerMockedValidator = new ApplicationController {
    override val desConnector = mockDesConnector
    override val jsonValidator = mockJsonValidator
    override val registrationHelper = mockRegistrationHelper
    override lazy val secureStorage = mockSecureStorage
    override def metrics: Metrics = Metrics
    override def auditService = mockAuditService
  }

  private def buildApplicationDetails = {
    val charity = Charity(
      id = Some("1"),
      name = Some("A Charity"),
      number = Some("234"),
      totalValue = Some(44.45)
    )

    val qualifyingBodies = QualifyingBody(
      id=Some("1"),
      name = Some("Qualifying Body"),
      totalValue = Some(12345)
    )

    ApplicationDetails(allAssets=None,
      propertyList=Nil,
      allLiabilities=None,
      allExemptions=None,
      charities= Seq(charity),
      qualifyingBodies = Seq(qualifyingBodies),
      widowCheck=None,
      increaseIhtThreshold=None,
      status= Constants.AppStatusInProgress,
      kickoutReason=None,
      ihtRef=Some("Abc123")
    )
  }

  val acknowledgementReference = AcknowledgeRefGenerator.getUUID

  val rd = CommonBuilder.buildRegistrationDetailsDODandDeceasedDetails
  when(mockRegistrationHelper.getRegistrationDetails(any(),any()))
    .thenReturn(Some(rd))

  val successHttpResponseForIhtReturn=HttpResponse(OK,Some(Json.parse(AcknowledgeRefGenerator.replacePlaceholderAckRefWithDefault(
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
  ))),Map())

  "Application Controller" must {
      "return JSON for application details on valid IHT Reference" in {
        when(mockSecureStorage.get(any(), any())).thenReturn(Some(Json.toJson(new ApplicationDetails)))

        val result = applicationController.get("", "IHT123", acknowledgementReference)(request)
        status(result) should be(OK)
      }

      "return empty application details with not started status" in {
        when(mockSecureStorage.get(any(), any())).thenReturn(None)

        val result = applicationController.get("", "IHT123", acknowledgementReference)(request)
        status(result) should be(OK)
        contentAsString(result) should include(Constants.AppStatusNotStarted)
      }

    "call persistence connector and return success on save" in {
      doNothing().when(mockSecureStorage).update("chicken", acknowledgementReference, Json.toJson(new ApplicationDetails))

      val applicationDetails = Json.toJson(buildApplicationDetails)

      val result = applicationController.save("", acknowledgementReference)(request.withBody(applicationDetails))

      status(result) should be(OK)
    }

    "call the audit service on save" in {
      implicit val headnapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
      implicit val exenapper = ArgumentCaptor.forClass(classOf[ExecutionContext])

      val adBefore = CommonBuilder.buildApplicationDetailsAllFields.copy(ihtRef = Some("IHT123"))
      val moneyOwedAfter = BasicEstateElement(Some(BigDecimal(50)))
      val optionAllAssetsAfter = adBefore.allAssets.map( _ copy (moneyOwed = Some(moneyOwedAfter)))
      val adAfter = adBefore.copy(allAssets = optionAllAssetsAfter)

      when(mockSecureStorage.get(any(), any())).thenReturn(Some(Json.toJson(adBefore)))
      when(mockAuditService.sendEvent(any(), any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val result = applicationController.save("IHT123", acknowledgementReference)(request.withBody(Json.toJson(adAfter)))

      val eventCaptorForString = ArgumentCaptor.forClass(classOf[String])
      val eventCaptorForMap = ArgumentCaptor.forClass(classOf[Map[String,String]])

      verify(mockAuditService).sendEvent(eventCaptorForString.capture,eventCaptorForMap.capture)(headnapper.capture, exenapper.capture)
      eventCaptorForString.getValue shouldBe "moneyOwed"
      eventCaptorForMap.getValue shouldBe Map(Constants.previousValue->"15",Constants.newValue->"50")
  }

    "call the audit service on save of gifts" in {
      implicit val headnapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
      implicit val exenapper = ArgumentCaptor.forClass(classOf[ExecutionContext])
      val beforeGiftsList = CommonBuilder.buildGiftsList
      val adBefore = CommonBuilder.buildApplicationDetailsAllFields.copy(ihtRef = Some("IHT123"), giftsList = Some(beforeGiftsList))
      val optionAllGifts = Seq(
        PreviousYearsGifts(
          yearId=Some("1"),
          value= Some(BigDecimal(1000)),
          exemptions = Some(BigDecimal(0)),
          startDate= Some("6 April 2004"),
          endDate= Some("5 April 2005")
        ),
        PreviousYearsGifts(
          yearId=Some("2"),
          value= Some(BigDecimal(2000)),
          exemptions = Some(BigDecimal(200)),
          startDate= Some("6 April 2005"),
          endDate= Some("5 April 2006")
        ),
        PreviousYearsGifts(
          yearId=Some("3"),
          value= Some(BigDecimal(2000)),
          exemptions = Some(BigDecimal(0)),
          startDate= Some("6 April 2006"),
          endDate= Some("5 April 2007")
        ),
        PreviousYearsGifts(
          yearId=Some("4"),
          value= Some(BigDecimal(4000)),
          exemptions = Some(BigDecimal(10)),
          startDate= Some("6 April 2007"),
          endDate= Some("5 April 2008")
        ),
        PreviousYearsGifts(
          yearId=Some("5"),
          value= Some(BigDecimal(5000)),
          exemptions = Some(BigDecimal(0)),
          startDate= Some("6 April 2008"),
          endDate= Some("5 April 2009")
        ),
        PreviousYearsGifts(
          yearId=Some("6"),
          value= Some(BigDecimal(6000)),
          exemptions = Some(BigDecimal(0)),
          startDate= Some("6 April 2009"),
          endDate= Some("5 April 2010")
        ),
        PreviousYearsGifts(
          yearId=Some("7"),
          value= Some(BigDecimal(8000)),
          exemptions = Some(BigDecimal(0)),
          startDate= Some("6 April 2010"),
          endDate= Some("5 April 2011")
        ))
      val adAfter = adBefore.copy(giftsList = Some(optionAllGifts))

      when(mockSecureStorage.get(any(), any())).thenReturn(Some(Json.toJson(adBefore)))
      when(mockAuditService.sendEvent(any(), any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val result = applicationController.save("IHT123", acknowledgementReference)(request.withBody(Json.toJson(adAfter)))

      val eventCaptorForString = ArgumentCaptor.forClass(classOf[String])
      val eventCaptorForMap = ArgumentCaptor.forClass(classOf[Map[String,String]])

      verify(mockAuditService).sendEvent(eventCaptorForString.capture,eventCaptorForMap.capture)(headnapper.capture, exenapper.capture)
      eventCaptorForString.getValue shouldBe "gifts"
      eventCaptorForMap.getValue shouldBe Map(Constants.previousValue->"27800",Constants.newValue->"27790")
    }

    val mockProcessingReport = mock[ProcessingReport]
    when(mockProcessingReport.isSuccess()).thenReturn(true)
    when(mockProcessingReport.iterator()).thenReturn(null)

    "return internal server error when valid front-end JSON submitted but failure response returned from DES with a reason" in {
      val incorrectIhtFailureJson = Json.parse( """{"failureResponse":{"reason":"IHT Case not found"}}""" )
      val errorHttpResponse = HttpResponse(INTERNAL_SERVER_ERROR,Some(incorrectIhtFailureJson),Map(),None)
      val jsonAD = Json.toJson(CommonBuilder.buildApplicationDetailsAllFields)

      when(mockJsonValidator.validate(any(), any())).thenReturn(mockProcessingReport)
      when(mockDesConnector.submitApplication(any(), any(), any()))
        .thenReturn(Future.successful(errorHttpResponse))
      val result = applicationControllerMockedValidator.submit("","")(request.withBody(jsonAD))
      status(result) should be(INTERNAL_SERVER_ERROR)
      //contentAsString(result) should include("Failure response received:")
    }

    "return internal server error when valid front-end JSON submitted but failure response returned from DES without a reason" in {
      val incorrectIhtFailureJson = Json.parse( """{"failureResponse":{"bla":"bla"}}""" )
      val errorHttpResponse = HttpResponse(INTERNAL_SERVER_ERROR,Some(incorrectIhtFailureJson),Map(),None)
      val jsonAD = Json.toJson(CommonBuilder.buildApplicationDetailsAllFields)

      when(mockJsonValidator.validate(any(), any())).thenReturn(mockProcessingReport)
      when(mockDesConnector.submitApplication(any(), any(), any()))
        .thenReturn(Future.successful(errorHttpResponse))
      val result = applicationControllerMockedValidator.submit("","")(request.withBody(jsonAD))
      status(result) should be(INTERNAL_SERVER_ERROR)
      //contentAsString(result) should include("Failure response received but no reason found")
    }

    "return internal server error when valid front-end JSON submitted but neither failure nor success response returned from DES" in {
      val incorrectIhtFailureJson = Json.parse( """{"bla":{"bla":"bla"}}""" )
      val errorHttpResponse = HttpResponse(INTERNAL_SERVER_ERROR,Some(incorrectIhtFailureJson),Map(),None)
      val jsonAD = Json.toJson(CommonBuilder.buildApplicationDetailsAllFields)



      when(mockJsonValidator.validate(any(), any())).thenReturn(mockProcessingReport)
      when(mockDesConnector.submitApplication(any(), any(), any()))
        .thenReturn(Future.successful(errorHttpResponse))

      val result = applicationControllerMockedValidator.submit("","")(request.withBody(jsonAD))
      status(result) should be(INTERNAL_SERVER_ERROR)
      //contentAsString(result) should include("Neither success nor failure response received from DES")
    }

    "return success response when valid front end JSON submitted" in {
      val correctIhtSuccessJson = Json.parse(
        """{"processingDate":"2001-12-17T09:30:47Z","returnId":"1234567890","versionNumber":"1234567890"}""" )
      val correctHttpResponse = HttpResponse(OK,Some(correctIhtSuccessJson),Map(),None)
      val jsonAD = Json.toJson(CommonBuilder.buildApplicationDetailsAllFields.copy(ihtRef = Some("12345678")))


      when(mockDesConnector.submitApplication(any(), any(), any()))
        .thenReturn(Future.successful(correctHttpResponse))
      val result = applicationController.submit("","")(request.withBody(jsonAD))
      status(result) should be(OK)
      assert(Metrics.successCounters(Api.SUB_APPLICATION).getCount>0, "Success counter for Sub Application Api is more than one")
    }

    "send an audit event containing the final estate value on a successful submission" in {
      implicit val headnapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
      implicit val exenapper = ArgumentCaptor.forClass(classOf[ExecutionContext])

      val correctIhtSuccessJson = Json.parse(
        """{"processingDate":"2001-12-17T09:30:47Z","returnId":"1234567890","versionNumber":"1234567890"}""" )
      val correctHttpResponse = HttpResponse(OK,Some(correctIhtSuccessJson),Map(),None)
      val jsonAD = Json.toJson(CommonBuilder.buildApplicationDetailsAllFields.copy(ihtRef = Some("12345678")))

      val eventCaptorForString = ArgumentCaptor.forClass(classOf[String])
      val eventCaptorForMap = ArgumentCaptor.forClass(classOf[Map[String,String]])

      when(mockDesConnector.submitApplication(any(), any(), any())).thenReturn(Future.successful(correctHttpResponse))
      when(mockAuditService.sendEvent(any(), any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val result = applicationController.submit("IHT123", "")(request.withBody(jsonAD))

      status(result) shouldBe OK

      verify(mockAuditService).sendEvent(eventCaptorForString.capture, eventCaptorForMap.capture)(headnapper.capture, exenapper.capture)
      eventCaptorForString.getValue shouldBe "finalEstateValue"
      eventCaptorForMap.getValue shouldBe Map("value"->"28090")
    }

    "return no content response when valid front end JSON submitted to real time risking" in {

      val ihtNoRulesFiredResponseJson = Json.parse (JsonLoader
        .fromResource("/json/des/risking/RiskOutcomeNoRulesFired.json").toString)

      val correctHttpResponse = HttpResponse(OK,Some(ihtNoRulesFiredResponseJson),Map(),None)


      when(mockRegistrationHelper.getRegistrationDetails(any(), any()))
        .thenReturn(Some(CommonBuilder.buildRegistrationDetailsDODandDeceasedDetails))

      when(mockDesConnector.submitRealtimeRisking(any(), any(), any()))
        .thenReturn(Future.successful(correctHttpResponse))
      val result = applicationController.getRealtimeRiskingMessage("","")(request)
      status(result) should be(NO_CONTENT)
    }

    "return no content response when valid front end JSON submitted to real time risking but rules fired in risking" in {

      val ihtRulesFiredResponseJson = Json.parse (JsonLoader
        .fromResource("/json/des/risking/RiskOutcomeRulesFiredNotInclRule2.json").toString)

      val correctHttpResponse = HttpResponse(OK,Some(ihtRulesFiredResponseJson),Map(),None)

      when(mockRegistrationHelper.getRegistrationDetails(any(), any()))
        .thenReturn(Some(CommonBuilder.buildRegistrationDetailsDODandDeceasedDetails))

      when(mockDesConnector.submitRealtimeRisking(any(), any(), any()))
        .thenReturn(Future.successful(correctHttpResponse))
      val result = applicationController.getRealtimeRiskingMessage("","")(request)
      status(result) should be(NO_CONTENT)
    }

    "return success response when valid front end JSON submitted to real time risking but rules fired in risking including rule 2 (money)" in {

      val ihtRulesFiredResponseJson = Json.parse (JsonLoader
        .fromResource("/json/des/risking/RiskOutcomeRulesFiredInclRule2.json").toString)

      val correctHttpResponse = HttpResponse(OK,Some(ihtRulesFiredResponseJson),Map(),None)

      when(mockRegistrationHelper.getRegistrationDetails(any(), any()))
        .thenReturn(Some(CommonBuilder.buildRegistrationDetailsDODandDeceasedDetails))

      when(mockDesConnector.submitRealtimeRisking(any(), any(), any()))
        .thenReturn(Future.successful(correctHttpResponse))
      val result = applicationController.getRealtimeRiskingMessage("","")(request)
      assert(Metrics.successCounters(Api.SUB_REAL_TIME_RISKING).getCount>0, "Success counter for Real Time Risking Api is more than one")
      status(result) should be(OK)
    }

    "return OK when clearance successfully requested" in {
      val correctIhtSuccessJson = Json.toJson("""{ "clearanceStatus": { "status": "Clearance Granted", "statusDate": "2015-04-29" }, "caseStatus":"Clearance Granted" }""")
      val correctHttpResponse = HttpResponse(OK,Some(correctIhtSuccessJson),Map())

      when(mockDesConnector.requestClearance(any(), any(), any()))
        .thenReturn(Future.successful(correctHttpResponse))

      val result = applicationController.requestClearance("","")(request)
      status(result) should be(OK)
      assert(Metrics.successCounters(Api.SUB_REQUEST_CLEARANCE).getCount>0, "Success counter for Request Clearance Api is more than one")
    }

    "correct Probate Details values are returned after parsing JS response " in {
      val json = Json.parse(TestHelper.JsSampleProbateDetails)
      val jsonValueAfterParsing: ProbateDetails = Json.fromJson((json \ "probateTotals").get)(probateDetailsReads)
        .getOrElse(throw new RuntimeException("Probate Details response not parsed properly"))

      assert(jsonValueAfterParsing.probateReference=="12345678A01-123","Probate Reference is AAA111222")
      assert(jsonValueAfterParsing.grossEstateforIHTPurposes== 123456.78,"grossEstateforIHTPurposes is 123456.78")

    }

    "Respond appropriately to a failure response while fetching Probate Details" in {
      when(mockDesConnector.getProbateDetails(any(),any(),any())).thenReturn((Future(errorHttpResponse)))
      val result = applicationController.getProbateDetails("","","")(request)
      status(result) should be(INTERNAL_SERVER_ERROR)
    }

    "Respond with no content if a Probate Detail is not available" in {
      when(mockDesConnector.getProbateDetails(any(),any(),any())).thenReturn((Future(noProbateDetailsHttpResponse)))
      val result = applicationController.getProbateDetails("","","")(request)
      status(result) should be(NO_CONTENT)
    }

    "Respond with OK on successful return of Probate Detail" in {
      when(mockDesConnector.getProbateDetails(any(),any(),any())).thenReturn(Future(successHttpResponseForProbateDetails))
      val result = applicationController.getProbateDetails("","","")(request)
      status(result) should be(OK)
      assert(Metrics.successCounters(Api.GET_PROBATE_DETAILS).getCount>0, "Success counter for Get Probate Details Api is more than one")
    }

    "Respond appropriately to a failure response while fetching IHT return details" in {
      when(mockDesConnector.getSubmittedApplicationDetails(any(),any(),any())).thenReturn((Future(errorHttpResponse)))
      val result = applicationController.getSubmittedApplicationDetails("","","")(request)
      status(result) should be(INTERNAL_SERVER_ERROR)
    }

    "Respond appropriately to a schema validation failure while fetching IHT return details" in {
      val incorrectIhtReturnJson = Json.toJson("""{ "SomeRubbish":"Not an IHT return" }""")
      val incorrectHttpResponse = HttpResponse(OK,Some(incorrectIhtReturnJson),Map())

      when(mockDesConnector.getSubmittedApplicationDetails(any(),any(),any())).thenReturn((Future(incorrectHttpResponse)))
      val result = applicationController.getSubmittedApplicationDetails("","","")(request)
      status(result) should be(INTERNAL_SERVER_ERROR)
    }

    "Respond appropriately when the IHT return details are not found" in {
      val noIhtReturnHttpResponse = HttpResponse(NOT_FOUND,None,Map(),None)

      when(mockDesConnector.getSubmittedApplicationDetails(any(),any(),any())).thenReturn((Future(noIhtReturnHttpResponse)))
      val result = applicationController.getSubmittedApplicationDetails("","","")(request)
      status(result) should be(INTERNAL_SERVER_ERROR)
    }

    "Respond with OK on successful return of IHT return details" in {
      when(mockDesConnector.getSubmittedApplicationDetails(any(),any(),any())).thenReturn(Future(successHttpResponseForIhtReturn))
      val result = applicationController.getSubmittedApplicationDetails("","","")(request)
      status(result) should be(OK)
      assert(Metrics.successCounters(Api.GET_APPLICATION_DETAILS).getCount>0, "Success counter for Get Application Details Api is more than one")
    }
  }
}
