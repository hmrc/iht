/*
 * Copyright 2021 HM Revenue & Customs
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

import constants.Constants
import models.application.assets.{AllAssets, Property}
import models.application.debts.AllLiabilities
import models.application.exemptions.{AllExemptions, Charity, QualifyingBody}
import models.application.gifts.{AllGifts, PreviousYearsGifts}
import models.application.tnrb.{TnrbEligibiltyModel, WidowCheck}
import play.api.libs.json.Json

// Should be in the bottom of the page due to object initialisation
case class ApplicationDetails(allAssets: Option[AllAssets] = None,
                              propertyList: List[Property] = Nil,
                              allLiabilities: Option[AllLiabilities] = None,
                              allExemptions: Option[AllExemptions] = None,
                              allGifts: Option[AllGifts] = None,
                              giftsList: Option[Seq[PreviousYearsGifts]] = None,
                              charities: Seq[Charity] = Seq(),
                              qualifyingBodies: Seq[QualifyingBody] = Seq(),
                              widowCheck: Option[WidowCheck] = None,
                              increaseIhtThreshold: Option[TnrbEligibiltyModel] = None,
                              status: String = Constants.AppStatusInProgress,
                              kickoutReason: Option[String] = None,
                              ihtRef: Option[String] = None,
                              reasonForBeingBelowLimit: Option[String] = None,
                              hasSeenExemptionGuidance: Option[Boolean] = Some(false)){

  def totalPropertyValue: BigDecimal = propertyList.map(_.value.getOrElse(BigDecimal(0))).sum

  def estateValue: BigDecimal = {
    if(totalExemptionsValue > 0) {
      totalAssetsValue + totalGiftsValue.getOrElse(0) - totalExemptionsValue - totalLiabilitiesValue
    } else {
      totalAssetsValue + totalGiftsValue.getOrElse(0)
    }
  }

  def totalGiftsValue: Option[BigDecimal] = {
    val seqOfPreviousYearsGifts = giftsList.getOrElse(Seq())
    val valueOfGifts = seqOfPreviousYearsGifts.flatMap(_.value).sum
    val valueOfExemptions = seqOfPreviousYearsGifts.flatMap(_.exemptions).sum
    Some(valueOfGifts - valueOfExemptions)
  }

  def totalAssetsValue: BigDecimal =
    allAssets.map(_.totalValueWithoutProperties()).getOrElse(BigDecimal(0)) + propertyList.map(_.value.getOrElse(BigDecimal(0))).sum

  def totalExemptionsValue: BigDecimal = charities.map(_.totalValue.getOrElse(BigDecimal(0))).sum +
    qualifyingBodies.map(_.totalValue.getOrElse(BigDecimal(0))).sum +
    allExemptions.flatMap(_.partner.map(_.totalAssets.getOrElse(BigDecimal(0)))).sum

  def totalLiabilitiesValue: BigDecimal = allLiabilities.map(_.totalValue()).getOrElse(BigDecimal(0))

}

object ApplicationDetails {
  implicit val formats = Json.format[ApplicationDetails]
}
