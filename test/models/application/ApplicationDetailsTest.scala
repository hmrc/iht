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

package models.application

import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.PlaySpec
import utils.{CommonBuilder, FakeIhtApp}

class ApplicationDetailsTest extends PlaySpec with FakeIhtApp with MockitoSugar{

  "ApplicationDetails" when {

    "totalGiftsValue is called" must {

      "return the correct value for total gifts where gifts are present" in {
        val ad = CommonBuilder.buildApplicationDetailsAllFields
        ad.totalGiftsValue mustBe Some(BigDecimal(28000 - 200))
      }

      "return the correct value for total gifts where gifts are not present" in {
        val ad = CommonBuilder.buildApplicationDetailsEmpty
        ad.totalGiftsValue mustBe Some(BigDecimal(0))
      }
    }

    "totalPropertyValue is called" must {

      "return the correct value of property" in {
        val ad = CommonBuilder.buildApplicationDetailsAllFields
        ad.totalPropertyValue mustBe BigDecimal(600)
      }
    }

    "totalAssetsValue is called" must {

      "return the correct value of total assets" in {
        val ad = CommonBuilder.buildApplicationDetailsAllFields
        ad.totalAssetsValue mustBe BigDecimal(771)
      }
    }


    "totalExemptionsValue is called" must {

      "return the correct value of total exemptions" in {
        val ad = CommonBuilder.buildApplicationDetailsAllFields
        ad.totalExemptionsValue mustBe BigDecimal(141)
      }
    }


    "totalLiabilitiesValue is called" must {

      "return the correct value of total liabilities" in {
        val ad = CommonBuilder.buildApplicationDetailsAllFields
        ad.totalLiabilitiesValue mustBe BigDecimal(340)
      }
    }


    "estateValue is called" must {

      "return the correct value when there are exemptions" in {
        val exemptions = CommonBuilder.buildAllExemptionsTotal3000
        val debts = CommonBuilder.buildAllLiabilities
        val ad = CommonBuilder.buildApplicationDetailsReasonForBeingBelowLimitExceptedEstate.copy(
          allExemptions = Some(exemptions), allLiabilities = Some(debts))
        ad.estateValue mustBe BigDecimal(121960 - 340)
      }

      "return the correct value when there are no exemptions and no debts in" in {
        val ad = CommonBuilder.buildApplicationDetailsReasonForBeingBelowLimitExceptedEstate
        ad.estateValue mustBe BigDecimal(121960 + 3000)
      }

      "return the correct value when there are debts but no exemptions " in {
        val debts = CommonBuilder.buildAllLiabilities
        val ad = CommonBuilder.buildApplicationDetailsReasonForBeingBelowLimitExceptedEstate.copy(
          allLiabilities = Some(debts))
        ad.estateValue mustBe BigDecimal(121960 + 3000)
      }
    }

  }
}
