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

import constants.Constants
import models.ApplicationDetails

object ModelHelper {

  val assetFields : Seq[(ApplicationDetails=>Option[BigDecimal], String)] = Seq(
    (_.propertyList.flatMap(_.value).reduceLeftOption(_ + _), Constants.properties),
    (_.allAssets.flatMap(_.vehicles).flatMap(_.value), Constants.motorVehicles),
    (_.allAssets.flatMap(_.vehicles).flatMap(_.shareValue), Constants.motorVehiclesShared),
    (_.allAssets.flatMap(_.privatePension).flatMap(_.value), Constants.privatePensions),
    (_.allAssets.flatMap(_.stockAndShare).flatMap(_.valueListed), Constants.stocksAndSharesListed),
    (_.allAssets.flatMap(_.stockAndShare).flatMap(_.valueNotListed), Constants.stocksAndSharesNotListed),
    (_.allAssets.flatMap(_.insurancePolicy).flatMap(_.value), Constants.insurancePolicies),
    (_.allAssets.flatMap(_.insurancePolicy).flatMap(_.shareValue), Constants.insurancePoliciesJointlyHeld),
    (_.allAssets.flatMap(_.businessInterest).flatMap(_.value), Constants.businessInterests),
    (_.allAssets.flatMap(_.nominated).flatMap(_.value), Constants.nominatedAssets),
    (_.allAssets.flatMap(_.heldInTrust).flatMap(_.value), Constants.assetsHeldInTrust),
    (_.allAssets.flatMap(_.foreign).flatMap(_.value), Constants.foreignAssets),
    (_.allAssets.flatMap(_.moneyOwed).flatMap(_.value), Constants.moneyOwed),
    (_.allAssets.flatMap(_.other).flatMap(_.value), Constants.otherAssets)
  )

  val debtFields : Seq[(ApplicationDetails=>Option[BigDecimal], String)] = Seq(
    (_.allLiabilities.flatMap(_.mortgages.flatMap(_.mortgageList.flatMap(_.value).reduceLeftOption(_ + _))), Constants.mortgages),
    (_.allLiabilities.flatMap(_.funeralExpenses).flatMap(_.value), Constants.funeralExpenses),
    (_.allLiabilities.flatMap(_.trust).flatMap(_.value), Constants.debtsOwedFromATrust),
    (_.allLiabilities.flatMap(_.debtsOutsideUk).flatMap(_.value), Constants.debtsOwedToAnyoneOutsideUK),
    (_.allLiabilities.flatMap(_.jointlyOwned).flatMap(_.value), Constants.debtsOwedOnJointlyOwnedAssets),
    (_.allLiabilities.flatMap(_.other).flatMap(_.value), Constants.otherDebts))

  val currencyFields: Seq[(ApplicationDetails=>Option[BigDecimal], String)] = assetFields ++ debtFields

  def currencyFieldDifferences(adBefore: ApplicationDetails, adAfter: ApplicationDetails): Map[String, Map[String, String]] = {
    if(adBefore == adAfter) {
      Map()
    } else {
      val fields = currencyFields.foldLeft(Map[String, Map[String, String]]()) {
        (currentValues: Map[String, Map[String, String]], fieldExpr: ((ApplicationDetails) => Option[BigDecimal], String)) =>
          val beforeValue = fieldExpr._1(adBefore)
          val afterValue = fieldExpr._1(adAfter)
          if (beforeValue != afterValue) {
            currentValues ++ Map(fieldExpr._2 -> Map(Constants.previousValue -> beforeValue.fold("")(_.toString),
              Constants.newValue -> afterValue.fold("")(_.toString)))
          } else {
            currentValues
          }
      }
      fields
    }
  }
}
