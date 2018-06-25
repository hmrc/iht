/*
 * Copyright 2018 HM Revenue & Customs
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

package config

import org.scalatest.mock.MockitoSugar
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{JsValidationException, NotFoundException}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import utils.exception.DESInternalServerError

class ApplicationGlobalTest extends UnitSpec with WithFakeApplication with MockitoSugar {

  val requestHeader = mock[RequestHeader]

  "On Error" should {
    "return 500 when DESInternalServerError" in {
      val exception = DESInternalServerError(new Exception("a generic application exception"))
      val result = ApplicationGlobal.onError(requestHeader, exception)

      status(result) shouldBe 500
    }
  }
}
