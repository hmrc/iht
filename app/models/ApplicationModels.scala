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

package models

import constants.Constants
import models.application.{TnrbEligibiltyModel, WidowCheck}
import org.joda.time.LocalDate
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads}
import utils.CommonHelper

trait EstateElement {
  val value: Option[BigDecimal]
}

trait Shareable {
  val shareValue: Option[BigDecimal]
}

trait ShareableEstateElement extends EstateElement with Shareable {
}

case class Property(id: Option[String], address: Option[UkAddress],  propertyType: Option[String], typeOfOwnership: Option[String], tenure: Option[String], value: Option[BigDecimal])

object Property {
  implicit val formats = Json.format[Property]
}

case class ShareableBasicEstateElement(value: Option[BigDecimal],
                                       shareValue: Option[BigDecimal],
                                       isOwned: Option[Boolean] = None,
                                       isOwnedShare: Option[Boolean] = None) extends ShareableEstateElement

object ShareableBasicEstateElement {
  implicit val formats = Json.format[ShareableBasicEstateElement]
}

case class BasicEstateElement(value: Option[BigDecimal],
                              isOwned: Option[Boolean] = None) extends EstateElement

case class BasicEstateElementLiabilities(isOwned: Option[Boolean], value: Option[BigDecimal]) extends EstateElement

object BasicEstateElementLiabilities {
  implicit val formats = Json.format[BasicEstateElementLiabilities]
}

object BasicEstateElement {
  implicit val formats = Json.format[BasicEstateElement]
}

case class PrivatePension(isChanged: Option[Boolean],
                          value: Option[BigDecimal],
                          isOwned: Option[Boolean] = None) extends EstateElement

object PrivatePension {
  implicit val formats = Json.format[PrivatePension]
}

case class StockAndShare(valueNotListed: Option[BigDecimal],
                         valueListed: Option[BigDecimal],
                         value: Option[BigDecimal],
                         isNotListed: Option[Boolean]= None,
                         isListed: Option[Boolean] = None) extends EstateElement

object StockAndShare {
  implicit val formats = Json.format[StockAndShare]
}

case class InsurancePolicy(isAnnuitiesBought: Option[Boolean],
                           isInsurancePremiumsPayedForSomeoneElse: Option[Boolean],
                           value: Option[BigDecimal],
                           shareValue: Option[BigDecimal],
                           policyInDeceasedName:Option[Boolean],
                           isJointlyOwned:Option[Boolean],
                           isInTrust:Option[Boolean],
                           coveredByExemption: Option[Boolean],
                           sevenYearsBefore: Option[Boolean],
                           moreThanMaxValue: Option[Boolean]
                            ) extends ShareableEstateElement

object InsurancePolicy {
  implicit val formats = Json.format[InsurancePolicy]
}

case class HeldInTrust(isMoreThanOne: Option[Boolean],
                       value: Option[BigDecimal],
                       isOwned: Option[Boolean] = None) extends EstateElement

object HeldInTrust {
  implicit val formats = Json.format[HeldInTrust]
}

case class Properties(isOwned: Option[Boolean])

object Properties {
  implicit val formats = Json.format[Properties]
}

case class Mortgage(id: String,
                    value: Option[BigDecimal],
                    isOwned: Option[Boolean] = None)

object Mortgage {
  implicit val formats = Json.format[Mortgage]
}

case class MortgageEstateElement(isOwned: Option[Boolean], mortgageList: List[Mortgage] = List.empty[Mortgage])

object MortgageEstateElement {
  implicit val formats = Json.format[MortgageEstateElement]
}

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
  implicit val formats = Json.format[AllAssets]

}

case class AllLiabilities(funeralExpenses: Option[BasicEstateElementLiabilities] = None,
                          trust: Option[BasicEstateElementLiabilities] = None,
                          debtsOutsideUk: Option[BasicEstateElementLiabilities] = None,
                          jointlyOwned: Option[BasicEstateElementLiabilities] = None,
                          other: Option[BasicEstateElementLiabilities] = None,
                          mortgages: Option[MortgageEstateElement] = None
                           ) {
  def totalValue(): BigDecimal = funeralExpenses.flatMap(_.value).getOrElse(BigDecimal(0)) +
    trust.flatMap(_.value).getOrElse(BigDecimal(0)) +
    debtsOutsideUk.flatMap(_.value).getOrElse(BigDecimal(0)) +
    jointlyOwned.flatMap(_.value).getOrElse(BigDecimal(0)) +
    other.flatMap(_.value).getOrElse(BigDecimal(0)) +
    mortgageValue

  def mortgageValue: BigDecimal = {
    val mort = mortgages.getOrElse(new MortgageEstateElement(Some(false), Nil)).mortgageList

    mort match {
      case x : List[Mortgage] if x.length > 0  => {
        x.flatMap(_.value).sum
      }
      case _ => BigDecimal(0)
    }
  }
}

object AllLiabilities {
  implicit val formats = Json.format[AllLiabilities]
}


//Exemptions Model starts
case class PartnerExemption(
                             isAssetForDeceasedPartner: Option[Boolean],
                             isPartnerHomeInUK: Option[Boolean],
                             firstName: Option[String],
                             lastName: Option[String],
                             dateOfBirth: Option[LocalDate],
                             nino: Option[String],
                             totalAssets: Option[BigDecimal])

object PartnerExemption {
  implicit val formats = Json.format[PartnerExemption]
}

case class BasicExemptionElement(isSelected: Option[Boolean])

object BasicExemptionElement {
  implicit val formats = Json.format[BasicExemptionElement]
}

case class Charity(id: Option[String],
                   name: Option[String],
                   number: Option[String],
                   totalValue: Option[BigDecimal])

object Charity {
  implicit val formats = Json.format[Charity]
}

case class QualifyingBody(id: Option[String],
                          name: Option[String],
                          totalValue: Option[BigDecimal])

object QualifyingBody {
  implicit val formats = Json.format[QualifyingBody]
}

case class AllExemptions(
                          partner: Option[PartnerExemption] = None,
                          charity: Option[BasicExemptionElement] = None,
                          qualifyingBody: Option[BasicExemptionElement] = None)

object AllExemptions {
  implicit val formats = Json.format[AllExemptions]
}

//Exemptions Model ends

// Gift Model start
case class AllGifts(
                     isGivenAway: Option[Boolean] = None,
                     isReservation: Option[Boolean],
                     isToTrust: Option[Boolean],
                     isGivenInLast7Years: Option[Boolean],
                     action: Option[String])

object AllGifts{
  implicit val formats = Json.format[AllGifts]
}

case class PreviousYearsGifts(
                               yearId: Option[String],
                               value: Option[BigDecimal],
                               exemptions: Option[BigDecimal],
                               startDate: Option[String],
                               endDate: Option[String]
                               )

object PreviousYearsGifts{
  implicit val formats = Json.format[PreviousYearsGifts]
}

//Gift Model end

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

  def totalGiftsValue: Option[BigDecimal] = {
    val seqOfPreviousYearsGifts = giftsList.getOrElse(Seq())
    val valueOfGifts = seqOfPreviousYearsGifts.flatMap(_.value).sum
    val valueOfExemptions = seqOfPreviousYearsGifts.flatMap(_.exemptions).sum
    Some(valueOfGifts - valueOfExemptions)
  }

  def totalPropertyValue:BigDecimal = propertyList.map(_.value.getOrElse(BigDecimal(0))).sum

  def totalPropertyValueForPropertyType(propertyType:String):BigDecimal =
    propertyList.filter(x => x.propertyType == propertyType).map(_.value.getOrElse(BigDecimal(0))).sum

  def totalAssetsValue:BigDecimal =
    allAssets.map(_.totalValueWithoutProperties).getOrElse(BigDecimal(0)) + propertyList.map(_.value.getOrElse(BigDecimal(0))).sum

  def totalExemptionsValue:BigDecimal = charities.map(_.totalValue.getOrElse(BigDecimal(0))).sum +
    qualifyingBodies.map(_.totalValue.getOrElse(BigDecimal(0))).sum +
    allExemptions.flatMap(_.partner.map(_.totalAssets.getOrElse(BigDecimal(0)))).sum

  def totalLiabilitiesValue:BigDecimal = allLiabilities.map(_.totalValue()).getOrElse(BigDecimal(0))

  def estateValue:BigDecimal = {
    if(totalExemptionsValue > 0) {
      totalAssetsValue + totalGiftsValue.getOrElse(0) - totalExemptionsValue - totalLiabilitiesValue
    } else {
      totalAssetsValue + totalGiftsValue.getOrElse(0)
    }
  }

}


object ApplicationDetails {
  implicit val formats = Json.format[ApplicationDetails]
}

case class ClearanceRequest(acknowledgmentReference: String, confirmRequest: Boolean = true)

object ClearanceRequest {
  implicit val formats = Json.format[ClearanceRequest]
}

// Probate Details Model
case class ProbateDetails(grossEstateforIHTPurposes : BigDecimal,
                          grossEstateforProbatePurposes: BigDecimal,
                          totalDeductionsForProbatePurposes: BigDecimal,
                          netEstateForProbatePurposes: BigDecimal,
                          valueOfEstateOutsideOfTheUK: BigDecimal,
                          valueOfTaxPaid: BigDecimal,
                          probateReference: String)

object ProbateDetails {

  implicit val probateDetailsReads: Reads[ProbateDetails]= (
    (JsPath \ "grossEstateforIHTPurposes").read[BigDecimal] and
      (JsPath \ "grossEstateforProbatePurposes").read[BigDecimal] and
      (JsPath \ "totalDeductionsForProbatePurposes").read[BigDecimal] and
      (JsPath \ "netEstateForProbatePurposes").read[BigDecimal].map{CommonHelper.isProbateNetValueNegative} and
      (JsPath \ "valueOfEstateOutsideOfTheUK").read[BigDecimal] and
      (JsPath \ "valueOfTaxPaid").read[BigDecimal] and
      (JsPath \ "probateReference").read[String]
    )(ProbateDetails.apply _)

  implicit val formats = Json.format[ProbateDetails]
}
