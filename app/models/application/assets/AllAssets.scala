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

import models.application.basicElements.{BasicEstateElement, ShareableBasicEstateElement}
import play.api.libs.json.{Json, OFormat}

case class AllAssets(action: Option[String],
                     money: Option[ShareableBasicEstateElement] = None,
                     household: Option[ShareableBasicEstateElement] = None,
                     vehicles: Option[ShareableBasicEstateElement] = None,
                     privatePension: Option[PrivatePension] = None,
                     stockAndShare: Option[StockAndShare] = None,
                     insurancePolicy: Option[InsurancePolicy] = None,
                     businessInterest: Option[BasicEstateElement] = None,
                     nominated: Option[BasicEstateElement] = None,
                     heldInTrust: Option[HeldInTrust] = None,
                     foreign: Option[BasicEstateElement] = None,
                     moneyOwed: Option[BasicEstateElement] = None,
                     other: Option[BasicEstateElement] = None,
                     properties: Option[Properties] = None) {

  def totalValueWithoutProperties(): BigDecimal = {
    val amountList = money.flatMap(_.value) ::
      household.flatMap(_.value) ::
      vehicles.flatMap(_.value) ::
      privatePension.flatMap(_.value) ::
      stockAndShare.flatMap(_.valueListed) ::
      stockAndShare.flatMap(_.valueNotListed)::
      insurancePolicy.flatMap(_.value) ::
      businessInterest.flatMap(_.value) ::
      moneyOwed.flatMap(_.value) ::
      nominated.flatMap(_.value) ::
      heldInTrust.flatMap(_.value) ::
      foreign.flatMap(_.value) ::
      other.flatMap(_.value) ::
      money.flatMap(_.shareValue) :: household.flatMap(_.shareValue) ::
      vehicles.flatMap(_.shareValue) ::
      insurancePolicy.flatMap(_.shareValue) ::
      Nil

    amountList.flatten.foldLeft(BigDecimal(0))(_ + _)
  }
}

object AllAssets {
  implicit val formats: OFormat[AllAssets] = Json.format[AllAssets]
}
