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

package json

import com.github.fge.jackson.JsonLoader
import uk.gov.hmrc.play.test.UnitSpec
import play.api.libs.json.Json
import utils.{AcknowledgeRefGenerator, NinoBuilder, CommonBuilder, TestHelper}

class JsonValidationTest  extends UnitSpec {
  "Validation" should {
    "be successful" in {
        val pr = JsonValidator.validate(Json.parse(
          AcknowledgeRefGenerator.replacePlaceholderAckRefWithDefault(
            NinoBuilder.replacePlaceholderNinoWithDefault(JsonLoader
          .fromResource("/json/validation/JsonTestValid.json").toString))),
          TestHelper.schemaPathRegistrationSubmission)
        pr.isSuccess should be (true)
    }
    "be unsuccessful if lead executor first name missing" in {
      val pr = JsonValidator.validate(Json.parse(NinoBuilder.replacePlaceholderNinoWithDefault(JsonLoader
        .fromResource("/json/validation/JsonTestInvalid1.json").toString)),TestHelper.schemaPathRegistrationSubmission)
      pr.isSuccess should be (false)
    }
    "be unsuccessful if lead executor post code missing" in {
      val pr = JsonValidator.validate(Json.parse(JsonLoader
        .fromResource("/json/validation/JsonTestInvalid2.json").toString), TestHelper.schemaPathRegistrationSubmission)
      pr.isSuccess should be (false)
    }

    "be unsuccessful if enumerated type case is wrong" in {
      val pr = JsonValidator.validate(Json.parse(JsonLoader
        .fromResource("/json/validation/JsonTestInvalid3.json").toString),TestHelper.schemaPathRegistrationSubmission)
      pr.isSuccess should be (false)
    }
  }
}
