package controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.WsScalaTestClient
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import util.{CommonBuilder, IntegrationSpec, TestData}

class ApplicationControllerSpec extends IntegrationSpec with WsScalaTestClient {

  implicit val wsClient = app.injector.instanceOf[WSClient]

  val ad = CommonBuilder.buildApplicationDetailsAllFields
  val requestBody = Json.toJson(ad)

  "Calling the submit method" should {
    "return a successful response" when {
      "no errors occur" in {
        val reference = "A0000A0000A0000"
        val nino = "AA123456A"

        mockGetCaseDetails(nino, reference, 200, TestData.validGetCaseDetails(nino, reference))
        mockIndividualsReturn(nino, reference, 200, TestData.successfulSubmissionResponse)

        val result = await(wsUrl(s"/iht/$nino/$reference/application/submit").post(requestBody))

        verify(getRequestedFor(urlPathMatching(s"/inheritance-tax/individuals/$nino/cases/$reference")))
        verify(postRequestedFor(urlPathMatching(s"/inheritance-tax/individuals/$nino/cases/$reference/returns"))
          .withRequestBody(equalToJson(TestData.sumissionRequestBody, false, true)))

        result.status shouldBe 200
        result.body shouldBe "Success response received: 12"

      }

      "getCaseDetails has a 500 status" in {
        val reference = "A0000A0000A0000"
        val nino = "AA123456A"

        mockGetCaseDetails(nino, reference, 500, TestData.validGetCaseDetails(nino, reference))
        mockIndividualsReturn(nino, reference, 200, TestData.successfulSubmissionResponse)

        val result = await(wsUrl(s"/iht/$nino/$reference/application/submit").post(requestBody))

        verify(getRequestedFor(urlPathMatching(s"/inheritance-tax/individuals/$nino/cases/$reference")))
        verify(0, postRequestedFor(urlPathMatching(s"/inheritance-tax/individuals/$nino/cases/$reference/returns")))

        result.status shouldBe 502
        result.body shouldBe "500 or 503 response returned from DES"
      }

      "getCaseDetails has a 503 status" in {
        val reference = "A0000A0000A0000"
        val nino = "AA123456A"

        mockGetCaseDetails(nino, reference, 503, TestData.validGetCaseDetails(nino, reference))
        mockIndividualsReturn(nino, reference, 200, TestData.successfulSubmissionResponse)

        val result = await(wsUrl(s"/iht/$nino/$reference/application/submit").post(requestBody))

        verify(getRequestedFor(urlPathMatching(s"/inheritance-tax/individuals/$nino/cases/$reference")))
        verify(0, postRequestedFor(urlPathMatching(s"/inheritance-tax/individuals/$nino/cases/$reference/returns")))

        result.status shouldBe 502
        result.body shouldBe "500 or 503 response returned from DES"

      }

      "individualReturn has a 503 status" in {
        val reference = "A0000A0000A0000"
        val nino = "AA123456A"

        mockGetCaseDetails(nino, reference, 200, TestData.validGetCaseDetails(nino, reference))
        mockIndividualsReturn(nino, reference, 503, TestData.successfulSubmissionResponse)

        val result = await(wsUrl(s"/iht/$nino/$reference/application/submit").post(requestBody))

        verify(getRequestedFor(urlPathMatching(s"/inheritance-tax/individuals/$nino/cases/$reference")))
        verify(postRequestedFor(urlPathMatching(s"/inheritance-tax/individuals/$nino/cases/$reference/returns"))
          .withRequestBody(equalToJson(TestData.sumissionRequestBody, false, true)))

        result.status shouldBe 502
        result.body shouldBe "500 or 503 response returned from DES"
      }

      "individualReturn has a 500 status" in {
        val reference = "A0000A0000A0000"
        val nino = "AA123456A"

        mockGetCaseDetails(nino, reference, 200, TestData.validGetCaseDetails(nino, reference))
        mockIndividualsReturn(nino, reference, 500, TestData.successfulSubmissionResponse)

        val result = await(wsUrl(s"/iht/$nino/$reference/application/submit").post(requestBody))

        verify(getRequestedFor(urlPathMatching(s"/inheritance-tax/individuals/$nino/cases/$reference")))
        verify(postRequestedFor(urlPathMatching(s"/inheritance-tax/individuals/$nino/cases/$reference/returns"))
          .withRequestBody(equalToJson(TestData.sumissionRequestBody, false, true)))

        result.status shouldBe 502
        result.body shouldBe "500 or 503 response returned from DES"
      }
    }
  }
}
