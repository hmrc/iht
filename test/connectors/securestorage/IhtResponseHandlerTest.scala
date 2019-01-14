/*
 * Copyright 2019 HM Revenue & Customs
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

package connectors.securestorage

import connectors.{IhtConnector, IhtResponseHandler}
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.http.{HttpResponse, Upstream5xxResponse}
import uk.gov.hmrc.play.test.UnitSpec
import utils.exception.DESInternalServerError
import uk.gov.hmrc.http._


class IhtResponseHandlerTest extends UnitSpec with MockitoSugar {

  def upstreamResponseMessage(verbName: String, url: String, status: Int, responseBody: String): String = {
    s"$verbName of '$url' returned $status. Response body: '$responseBody'"
  }

  "handleIhtResponse" should  {
    "return DESInternalServerError when status is 500" in {
      val response = HttpResponse(500)
      def result(): HttpResponse = IhtResponseHandler.handleIhtResponse("test method", "test url", response)
      val expectedException = DESInternalServerError(Upstream5xxResponse(upstreamResponseMessage("test method", "test url", response.status, response.body), response.status, 502))

      the[DESInternalServerError] thrownBy result shouldBe expectedException
    }

    "return Upstream4xxResponse when status is 405" in {
      val response = HttpResponse(405)
      def result(): HttpResponse = IhtResponseHandler.handleIhtResponse("test method", "test url", response)
      val expectedException = Upstream4xxResponse(upstreamResponseMessage("test method", "test url", response.status, null), response.status, 500)

      the[Upstream4xxResponse] thrownBy result shouldBe expectedException

    }
  }

}
