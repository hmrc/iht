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

object AuditHelper {

  private val assetFields : Seq[(ApplicationDetails=>Option[BigDecimal], String)] = Seq(
    (_.propertyList.flatMap(_.value).reduceLeftOption(_ + _), Constants.AuditTypeProperties),
    (_.allAssets.flatMap(_.money).flatMap(_.value), Constants.AuditTypeMoney),
    (_.allAssets.flatMap(_.money).flatMap(_.shareValue), Constants.AuditTypeMoneyShared),
    (_.allAssets.flatMap(_.household).flatMap(_.value), Constants.AuditTypeHousehold),
    (_.allAssets.flatMap(_.household).flatMap(_.shareValue), Constants.AuditTypeHouseholdShared),
    (_.allAssets.flatMap(_.vehicles).flatMap(_.value), Constants.AuditTypeMotorVehicles),
    (_.allAssets.flatMap(_.vehicles).flatMap(_.shareValue), Constants.AuditTypeMotorVehiclesShared),
    (_.allAssets.flatMap(_.privatePension).flatMap(_.value), Constants.AuditTypePrivatePensions),
    (_.allAssets.flatMap(_.stockAndShare).flatMap(_.valueListed), Constants.AuditTypeStocksAndSharesListed),
    (_.allAssets.flatMap(_.stockAndShare).flatMap(_.valueNotListed), Constants.AuditTypeStocksAndSharesNotListed),
    (_.allAssets.flatMap(_.insurancePolicy).flatMap(_.value), Constants.AuditTypeInsurancePolicies),
    (_.allAssets.flatMap(_.insurancePolicy).flatMap(_.shareValue), Constants.AuditTypeInsurancePoliciesJointlyHeld),
    (_.allAssets.flatMap(_.businessInterest).flatMap(_.value), Constants.AuditTypeBusinessInterests),
    (_.allAssets.flatMap(_.nominated).flatMap(_.value), Constants.AuditTypeNominatedAssets),
    (_.allAssets.flatMap(_.heldInTrust).flatMap(_.value), Constants.AuditTypeAssetsHeldInTrust),
    (_.allAssets.flatMap(_.foreign).flatMap(_.value), Constants.AuditTypeForeignAssets),
    (_.allAssets.flatMap(_.moneyOwed).flatMap(_.value), Constants.AuditTypeMoneyOwed),
    (_.allAssets.flatMap(_.other).flatMap(_.value), Constants.AuditTypeOtherAssets)
  )

  private val debtFields : Seq[(ApplicationDetails=>Option[BigDecimal], String)] = Seq(
    (_.allLiabilities.flatMap(_.mortgages.flatMap(_.mortgageList.flatMap(_.value).reduceLeftOption(_ + _))), Constants.AuditTypeMortgages),
    (_.allLiabilities.flatMap(_.funeralExpenses).flatMap(_.value), Constants.AuditTypeFuneralExpenses),
    (_.allLiabilities.flatMap(_.trust).flatMap(_.value), Constants.AuditTypeDebtsOwedFromATrust),
    (_.allLiabilities.flatMap(_.debtsOutsideUk).flatMap(_.value), Constants.AuditTypeDebtsOwedToAnyoneOutsideUK),
    (_.allLiabilities.flatMap(_.jointlyOwned).flatMap(_.value), Constants.AuditTypeDebtsOwedOnJointlyOwnedAssets),
    (_.allLiabilities.flatMap(_.other).flatMap(_.value), Constants.AuditTypeOtherDebts))

  private val exemptionFields : Seq[(ApplicationDetails=>Option[BigDecimal], String)] = Seq(
    (_.charities.flatMap(_.totalValue).reduceLeftOption(_ + _), Constants.AuditTypeExemptionCharities),
    (_.qualifyingBodies.flatMap(_.totalValue).reduceLeftOption(_ + _), Constants.AuditTypeExemptionQualfifyingBodies),
    (_.allExemptions.flatMap(_.partner).flatMap(_.totalAssets), Constants.AuditTypeExemptionPartner)
  )

  private val giftsFields  : Seq[(ApplicationDetails=>Option[BigDecimal], String)] = Seq(
    (_.totalGiftsValue, Constants.AuditTypeGifts)
  )

  private val currencyFields: Seq[(ApplicationDetails=>Option[BigDecimal], String)] = assetFields ++ debtFields ++ exemptionFields ++ giftsFields

  def currencyFieldDifferences(adBefore: ApplicationDetails, adAfter: ApplicationDetails): Map[String, Map[String, String]] = {
    if(adBefore == adAfter) {
      Map()
    } else {
      val fields = currencyFields.foldLeft(Map[String, Map[String, String]]()) {
        (currentValues: Map[String, Map[String, String]], fieldExpr: ((ApplicationDetails) => Option[BigDecimal], String)) =>
          val beforeValue = fieldExpr._1(adBefore)
          val afterValue = fieldExpr._1(adAfter)
          if (beforeValue != afterValue) {
            currentValues ++ Map(fieldExpr._2 -> Map(fieldExpr._2 + " " + Constants.AuditTypePreviousValue -> beforeValue.fold("")(_.toString),
              fieldExpr._2 + " " + Constants.AuditTypeNewValue -> afterValue.fold("")(_.toString)))
          } else {
            currentValues
          }
      }
      fields
    }
  }
}
