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

import models.BasicEstateElement
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

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
      def appDetails(moneyOwned: Int) = CommonBuilder.buildApplicationDetailsAllFields.copy(
        allAssets = Some(CommonBuilder.buildAllAssets.copy(
          moneyOwed = Some(BasicEstateElement(Some(moneyOwned), Some(true))))))

      val beforeUpdate = appDetails(100)
      val afterUpdate = appDetails(1000)

      val differences: Map[String, Map[String, String]] = ModelHelper.currencyFieldDifferences(beforeUpdate, afterUpdate)
      differences shouldBe Map("moneyOwed" -> Map("old" -> "100", "new" -> "1000"))
    }

  }

}
