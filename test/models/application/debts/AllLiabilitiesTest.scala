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

package models.application.debts

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import utils.{CommonBuilder, FakeIhtApp}

class AllLiabilitiesTest extends UnitSpec with FakeIhtApp with MockitoSugar{

  "AllLiabilities" when {

    "totalValue is called" must {

      "return the correct value for total liabilities" in {
        val ad = CommonBuilder.buildApplicationDetailsAllFields
        ad.allLiabilities.fold(BigDecimal(0))(_.totalValue) shouldBe BigDecimal(340)
      }

      "return the total as 0 if there are no liabilities in the estate " in {
        val ad = CommonBuilder.buildApplicationDetailsEmpty
        ad.allLiabilities.fold(BigDecimal(0))(_.totalValue) shouldBe BigDecimal(0)
      }
    }


    "mortgageValue is called" must {

      "return the correct value for mortgage" in {
        val ad = CommonBuilder.buildApplicationDetailsAllFields
        ad.allLiabilities.fold(BigDecimal(0))(_.mortgageValue) shouldBe BigDecimal(230)
      }

      "return the mortgage value ss 0 if there are no mortgage in the estate " in {
        val ad = CommonBuilder.buildApplicationDetailsEmpty
        ad.allLiabilities.fold(BigDecimal(0))(_.mortgageValue) shouldBe BigDecimal(0)
      }
    }

  }

}
