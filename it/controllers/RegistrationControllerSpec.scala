package controllers

import com.github.tomakehurst.wiremock.client.WireMock.{getRequestedFor, postRequestedFor, urlPathMatching, verify}
import org.scalatestplus.play.WsScalaTestClient
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import util.{CommonBuilder, IntegrationSpec, TestData}

class RegistrationControllerSpec extends IntegrationSpec with WsScalaTestClient {

  implicit val wsClient = app.injector.instanceOf[WSClient]

  val rd = CommonBuilder.JsSampleCaseDetailsString
  val requestBody = Json.parse(rd)

  "Calling the submit method" should {
    "return a successful response" when {
      "no errors occur" in {
        val nino = "AA123456A"
        val reference = "A0000A0000A0000"
        val correctIhtReferenceNoJs = Json.parse( """{"referenceNumber":"AAA111222"}""" )

        mockAuth(nino, 200)
        mockGetCase(nino,  200, """{"referenceNumber":"AAA111222"}""")

        val result = await(wsCall(controllers.registration.routes.RegistrationController.submit(nino)).post(requestBody))

        result.status shouldBe 200
        result.body shouldBe "AAA111222"

      }

      "getCase has a 500 status" in {
        val nino = "AA123456A"
        val reference = "A0000A0000A0000"
        val correctIhtReferenceNoJs = Json.parse( """{"referenceNumber":"AAA111222"}""" )

        mockAuth(nino, 200)
        mockGetCase(nino,  500, """{"referenceNumber":"AAA111222"}""")

        val result = await(wsCall(controllers.registration.routes.RegistrationController.submit(nino)).post(requestBody))

        result.status shouldBe 502
        result.body shouldBe "500 response returned from DES"
      }

      "getCase has a 503 status" in {
        val nino = "AA123456A"
        val reference = "A0000A0000A0000"
        val correctIhtReferenceNoJs = Json.parse( """{"referenceNumber":"AAA111222"}""" )

        mockAuth(nino, 200)
        mockGetCase(nino,  503, """{"referenceNumber":"AAA111222"}""")

        val result = await(wsCall(controllers.registration.routes.RegistrationController.submit(nino)).post(requestBody))

        result.status shouldBe 502
        result.body shouldBe "{\"statusCode\":502,\"message\":\"POST of 'http://localhost:11111/inheritance-tax/individuals/AA123456A/cases/' returned 503. Response body: '{\\\"referenceNumber\\\":\\\"AAA111222\\\"}'\"}"
      }
    }
  }

}
