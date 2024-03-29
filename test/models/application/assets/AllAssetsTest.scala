/*
 * Copyright 2022 HM Revenue & Customs
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

package models.application.assets

import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import utils.{CommonBuilder, FakeIhtApp}

class AllAssetsTest extends PlaySpec with FakeIhtApp with MockitoSugar{

  "AllAssets" when {

    "totalValueWithoutProperties is called" must {

      "return the correct value for total assets excluding properties" in {
        val ad = CommonBuilder.buildApplicationDetailsAllFields
        ad.allAssets.fold(BigDecimal(0))(_.totalValueWithoutProperties) mustBe BigDecimal(171)
      }

      "return the total as 0 if there are no assets in the estate " in {
        val ad = CommonBuilder.buildApplicationDetailsEmpty
        ad.allAssets.fold(BigDecimal(0))(_.totalValueWithoutProperties) mustBe BigDecimal(0)
      }
    }
  }

}
