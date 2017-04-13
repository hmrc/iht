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

import models.ApplicationDetails

object ModelHelper {

  val currencyFields: Seq[(ApplicationDetails=>Option[BigDecimal], String)] = Seq(
    (
      _.allAssets.flatMap(_.moneyOwed).flatMap(_.value),
      "moneyOwed"
    )
  )

  def currencyFieldDifferences(adBefore: ApplicationDetails, adAfter: ApplicationDetails): Map[String, Map[String, String]] = {
    if(adBefore == adAfter) {
      Map()
    } else {
      var fields = currencyFields.foldLeft(Map[String, Map[String, String]]()) {
        (currentValues: Map[String, Map[String, String]], fieldExpr: ((ApplicationDetails) => Option[BigDecimal], String)) =>
        val beforeValue = fieldExpr._1(adBefore)
        val afterValue = fieldExpr._1(adAfter)
        if (beforeValue != afterValue) {
          currentValues ++ Map(fieldExpr._2 -> Map("old" -> beforeValue.fold("")(_.toString),
            "new" -> afterValue.fold("")(_.toString)))
        } else {
          currentValues
        }
      }
      fields
    }
  }
}
