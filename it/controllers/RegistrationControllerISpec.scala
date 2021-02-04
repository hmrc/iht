package controllers

import org.scalatestplus.play.WsScalaTestClient
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import util.{CommonBuilder, IntegrationSpec}

class RegistrationControllerISpec extends IntegrationSpec with WsScalaTestClient {

  implicit val wsClient = app.injector.instanceOf[WSClient]

  val rd = CommonBuilder.JsSampleCaseDetailsString
  val requestBody = Json.parse(rd)

  "Calling the submit method" should {
    "return a successful response" when {
      "no errors occur" in {
        val nino = "AA123456A"

        mockAuth(nino, 200)
        mockGetCase(nino,  200, """{"referenceNumber":"AAA111222"}""")

        val result = await(wsCall(controllers.registration.routes.RegistrationController.submit(nino)).post(requestBody))

        result.status mustBe 200
        result.body mustBe "AAA111222"

      }

      "getCase has a 500 status" in {
        val nino = "AA123456A"

        mockAuth(nino, 200)
        mockGetCase(nino,  500, """{"referenceNumber":"AAA111222"}""")

        val result = await(wsCall(controllers.registration.routes.RegistrationController.submit(nino)).post(requestBody))

        result.status mustBe 502
        result.body mustBe "500 or 503 response returned from DES"
      }

      "getCase has a 503 status" in {
        val nino = "AA123456A"

        mockAuth(nino, 200)
        mockGetCase(nino,  503, """{"referenceNumber":"AAA111222"}""")

        val result = await(wsCall(controllers.registration.routes.RegistrationController.submit(nino)).post(requestBody))

        result.status mustBe 502
        result.body mustBe "500 or 503 response returned from DES"
      }
    }
  }

}
