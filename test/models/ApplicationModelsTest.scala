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

package models

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import utils.{CommonBuilder, FakeIhtApp}

class ApplicationModelsTest extends UnitSpec with FakeIhtApp with MockitoSugar{

  "ApplicationModels" when {

    "totalGiftsValue is called" must {

      "return the correct value for total gifts where gifts are present" in {
        val ad = CommonBuilder.buildApplicationDetailsAllFields
        ad.totalGiftsValue shouldBe Some(BigDecimal(28000 - 200))
      }

      "return the correct value for total gifts where gifts are not present" in {
        val ad = CommonBuilder.buildApplicationDetailsEmpty
        ad.totalGiftsValue shouldBe Some(BigDecimal(0))
      }

    }

  }

}
