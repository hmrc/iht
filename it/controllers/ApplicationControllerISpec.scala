package controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import models.application.ApplicationDetails
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.WsScalaTestClient
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import util.{CommonBuilder, IntegrationSpec, TestData}

class ApplicationControllerISpec extends IntegrationSpec with WsScalaTestClient with BeforeAndAfterEach {

  implicit val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val ad: ApplicationDetails = CommonBuilder.buildApplicationDetailsAllFields
  val requestBody: JsValue = Json.toJson(ad)

  "Calling the submit method" should {
    "return a successful response" when {
      "no errors occur" in {
        val reference = "A0000A0000A0000"
        val nino = "AA123456A"

        mockGetCaseDetails(nino, reference, 200, TestData.validGetCaseDetails(nino, reference))
        mockIndividualsReturn(nino, reference, 200, TestData.successfulSubmissionResponse)

        val result = await(wsUrl(s"/iht/$nino/$reference/application/submit").post(requestBody))

        verify(getRequestedFor(urlPathMatching(s"/inheritance-tax/individuals/$nino/cases/$reference")))

        result.status mustBe 200
        result.body mustBe "Success response received: 12"

      }

      "getCaseDetails has a 500 status" in {
        val reference = "A0000A0000A0000"
        val nino = "AA123456A"

        mockGetCaseDetails(nino, reference, 500, TestData.validGetCaseDetails(nino, reference))
        mockIndividualsReturn(nino, reference, 200, TestData.successfulSubmissionResponse)

        val result = await(wsUrl(s"/iht/$nino/$reference/application/submit").post(requestBody))

        verify(getRequestedFor(urlPathMatching(s"/inheritance-tax/individuals/$nino/cases/$reference")))

        result.status mustBe 502
        result.body mustBe "500 or 503 response returned from DES"
      }

      "getCaseDetails has a 503 status" in {
        val reference = "A0000A0000A0000"
        val nino = "AA123456A"

        mockGetCaseDetails(nino, reference, 503, TestData.validGetCaseDetails(nino, reference))
        mockIndividualsReturn(nino, reference, 200, TestData.successfulSubmissionResponse)

        val result = await(wsUrl(s"/iht/$nino/$reference/application/submit").post(requestBody))

        verify(getRequestedFor(urlPathMatching(s"/inheritance-tax/individuals/$nino/cases/$reference")))

        result.status mustBe 502
        result.body mustBe "500 or 503 response returned from DES"

      }

      "individualReturn has a 503 status" in {
        val reference = "A0000A0000A0000"
        val nino = "AA123456A"

        mockGetCaseDetails(nino, reference, 200, TestData.validGetCaseDetails(nino, reference))
        mockIndividualsReturn(nino, reference, 503, TestData.successfulSubmissionResponse)

        val result = await(wsUrl(s"/iht/$nino/$reference/application/submit").post(requestBody))

        verify(getRequestedFor(urlPathMatching(s"/inheritance-tax/individuals/$nino/cases/$reference")))

        result.status mustBe 502
        result.body mustBe "500 or 503 response returned from DES"
      }

      "individualReturn has a 500 status" in {
        val reference = "A0000A0000A0000"
        val nino = "AA123456A"

        mockGetCaseDetails(nino, reference, 200, TestData.validGetCaseDetails(nino, reference))
        mockIndividualsReturn(nino, reference, 500, TestData.successfulSubmissionResponse)

        val result = await(wsUrl(s"/iht/$nino/$reference/application/submit").post(requestBody))

        verify(getRequestedFor(urlPathMatching(s"/inheritance-tax/individuals/$nino/cases/$reference")))
        
        result.status mustBe 502
        result.body mustBe "500 or 503 response returned from DES"
      }
    }
  }
}
