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

import connectors._
import connectors.securestorage.SecureStorage
import metrics.Metrics
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json._
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import utils._
import constants.Constants
import models._


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import json.JsonValidator
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.ProcessingReport
import models.enums.Api

class ApplicationControllerTest extends UnitSpec with FakeIhtApp with MockitoSugar {

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

  def applicationController = new ApplicationController {

    override val desConnector = mockDesConnector
    override val jsonValidator = JsonValidator
    override val registrationHelper = mockRegistrationHelper
    override lazy val secureStorage = mockSecureStorage
    override def metrics: Metrics = Metrics
  }

  def applicationControllerMockedValidator = new ApplicationController {
    override val desConnector = mockDesConnector
    override val jsonValidator = mockJsonValidator
    override val registrationHelper = mockRegistrationHelper
    override lazy val secureStorage = mockSecureStorage
    override def metrics: Metrics = Metrics
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
    import com.github.fge.jsonschema.core.report.ProcessingMessage
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

//    "call persistence connector and return error if save not successful" in {
//      doThrow(new RuntimeException()).when(mockSecureStorage).update("chicken", "bigkey", Json.toJson(new ApplicationDetails))
//
//      val applicationDetails = Json.toJson(buildApplicationDetails)
//
//      val result = applicationController.save()(request.withBody(applicationDetails))
//      status(result) should be(INTERNAL_SERVER_ERROR)
//    }

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
      val jsonValueAfterParsing = Json.fromJson[ProbateDetails](json \ "probateTotals").getOrElse(throw new RuntimeException("Probate Details response not parsed properly"))

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
