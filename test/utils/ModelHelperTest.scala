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

package utils

import models.{BasicEstateElement, BasicEstateElementLiabilities}
import models.application.WidowCheck
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import org.joda.time.LocalDate
import constants.Constants

class ModelHelperTest extends UnitSpec with FakeIhtApp with MockitoSugar {

  "ModelHelper" must {

    "return an empty Map for two identical ApplicationDetails objects" in {
      val beforeUpdate = CommonBuilder.buildApplicationDetailsAllFields
      val afterUpdate = CommonBuilder.buildApplicationDetailsAllFields
      val differences = ModelHelper.currencyFieldDifferences(beforeUpdate, afterUpdate)
      differences shouldBe Map()
    }

    "return, as a Map[String, Map[String,String]] the differences between " +
      "the currency values in two ApplicationDetails objects" in {
      def appDetails(moneyOwed: Int) = CommonBuilder.buildApplicationDetailsAllFields.copy(
        allAssets = Some(CommonBuilder.buildAllAssets.copy(
          moneyOwed = Some(BasicEstateElement(Some(moneyOwed), Some(true))))))

      val beforeUpdate = appDetails(100)
      val afterUpdate = appDetails(1000)

      val differences: Map[String, Map[String, String]] = ModelHelper.currencyFieldDifferences(beforeUpdate, afterUpdate)
      differences shouldBe Map(Constants.moneyOwed -> Map(Constants.previousValue -> "100", Constants.newValue -> "1000"))
    }

    "return an empty Map for two different ApplicationDetails objects when the differences relate" +
      "to non-currency values" in {
      def appDetails(date: LocalDate) = CommonBuilder.buildApplicationDetailsAllFields.copy(widowCheck = Some(WidowCheck(
        widowed = Some(true), dateOfPreDeceased = Some(date))))

      val beforeUpdate = appDetails(new LocalDate(2015, 10, 10))
      val afterUpdate = appDetails(new LocalDate(2015, 11, 10))
      val differences = ModelHelper.currencyFieldDifferences(beforeUpdate, afterUpdate)
      differences shouldBe Map()
    }

    "return a Map containing a changed currency field but not a changed non-currency field" in {
      def appDetails(moneyOwed: Int, date: LocalDate) = CommonBuilder.buildApplicationDetailsAllFields.copy(widowCheck = Some(WidowCheck(
        widowed = Some(true), dateOfPreDeceased = Some(date))), allAssets = Some(CommonBuilder.buildAllAssets.copy(
        moneyOwed = Some(BasicEstateElement(Some(moneyOwed), Some(true))))))

      val beforeUpdate = appDetails(100, new LocalDate(2015, 10, 10))
      val afterUpdate = appDetails(1000, new LocalDate(2015, 11, 10))

      val differences = ModelHelper.currencyFieldDifferences(beforeUpdate, afterUpdate)
      differences shouldBe Map(Constants.moneyOwed -> Map(Constants.previousValue -> "100", Constants.newValue -> "1000"))
    }

    "return a Map containing two changed currency fields" in {
      def appDetails(moneyOwed: Int, otherDebts: Int) = CommonBuilder.buildApplicationDetailsAllFields.copy(
        allAssets = Some(CommonBuilder.buildAllAssets.copy(
        moneyOwed = Some(BasicEstateElement(Some(moneyOwed), Some(true))))),
        allLiabilities = Some(CommonBuilder.buildAllLiabilities.copy(
          other = Some(BasicEstateElementLiabilities(Some(true), Some(BigDecimal(otherDebts))))
        )))

      val beforeUpdate = appDetails(100, 1000)
      val afterUpdate = appDetails(1000, 100)

      val differences = ModelHelper.currencyFieldDifferences(beforeUpdate, afterUpdate)
      differences shouldBe Map(
        Constants.moneyOwed -> Map(Constants.previousValue -> "100", Constants.newValue -> "1000"),
        Constants.otherDebts -> Map(Constants.previousValue -> "1000", Constants.newValue -> "100")
      )
    }

  }

}
