/*
 * Copyright 2021 HM Revenue & Customs
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

import connectors.IhtResponseHandler
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.{HttpResponse, Upstream5xxResponse, _}
import utils.exception.DESInternalServerError
import play.api.http.Status._


class IhtResponseHandlerTest extends PlaySpec with MockitoSugar {

  def upstreamResponseMessage(verbName: String, url: String, status: Int, responseBody: String): String = {
    s"$verbName of '$url' returned $status. Response body: '$responseBody'"
  }

  "handleIhtResponse" must  {
    "return DESInternalServerError when status is 500" in {
      val response = HttpResponse(INTERNAL_SERVER_ERROR, "")
      def result(): HttpResponse = IhtResponseHandler.handleIhtResponse("test method", "test url", response).right.get
      val expectedException = DESInternalServerError(UpstreamErrorResponse(
        upstreamResponseMessage("test method", "test url", response.status, response.body), response.status, BAD_GATEWAY))

      the[DESInternalServerError] thrownBy result mustBe expectedException
    }

    "return Upstream4xxResponse when status is 405" in {
      val response = HttpResponse(METHOD_NOT_ALLOWED, "")
      def result() = IhtResponseHandler.handleIhtResponse("test method", "test url", response).left.get
      val expectedException = UpstreamErrorResponse(upstreamResponseMessage(
        "test method", "test url", response.status, response.body), response.status, INTERNAL_SERVER_ERROR)

      result() mustBe expectedException

    }
  }

}
