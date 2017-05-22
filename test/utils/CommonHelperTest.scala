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

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec


class CommonHelperTest extends UnitSpec with FakeIhtApp with MockitoSugar  {
  "dateLongFormatToDesString" must {
    "Convert a valid date" in {
      val s = CommonHelper.dateLongFormatToDesString("1 January 2000")
      s shouldBe "2000-01-01"
    }

    "Convert another valid date" in {
      val s = CommonHelper.dateLongFormatToDesString("5 April 2005")
      s shouldBe "2005-04-05"
    }

    "throw an exception if asked to convert an invalid date" in {
      a[java.lang.RuntimeException] should be thrownBy {
        CommonHelper.dateLongFormatToDesString("1 January")
      }
    }

    "throw an exception if asked to convert an invalid date (invalid month)" in {
      a[java.lang.RuntimeException] should be thrownBy {
        CommonHelper.dateLongFormatToDesString("1 Bloop 2025")
      }
    }

  }

  "dateFormatChangeToPadZeroInDayAndMonth" must {
    "Pad 0 to month and day of a valid date" in {
      val s = CommonHelper.dateFormatChangeToPadZeroToDayAndMonth("2015-1-2")
      s shouldBe "2015-01-02"
    }

    "Dont pad 0 to month and day when valid date" in {
      val s = CommonHelper.dateFormatChangeToPadZeroToDayAndMonth("2014-12-10")
      s shouldBe "2014-12-10"
    }

    "throw an exception if asked to convert an invalid date" in {
      a[java.lang.RuntimeException] should be thrownBy {
        CommonHelper.dateFormatChangeToPadZeroToDayAndMonth("2014-1")
      }
    }
  }

  "a negative net estate value should not be negative" must {
    "A negitive value should be converted to 0" in {
      val value = CommonHelper.isProbateNetValueNegative(BigDecimal(-2333))
      value shouldBe(BigDecimal(0))
    }

    "A positive value should not be converted" in {
      val value = CommonHelper.isProbateNetValueNegative(BigDecimal(12334545))
      value shouldBe(BigDecimal(12334545))
    }

  }
}
