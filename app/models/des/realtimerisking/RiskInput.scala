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

package models.des.realtimerisking
import play.api.libs.json.Json
import org.joda.time.LocalDate
import constants.{AssetDetails, Constants}
import models._
import org.joda.time.LocalDateTime

case class Address(addressLine1: Option[String]=None, addressLine2: Option[String]=None,
                   addressLine3: Option[String], addressLine4: Option[String],
                   postalCode: Option[String]=None, countryCode: Option[String]=None)

object Address {
  implicit val formats = Json.format[Address]
}

case class SpousesEstate(domiciledInUk: Option[Boolean]=None, whollyExempt: Option[Boolean]=None,
                         jointAssetsPassingToOther: Option[Boolean]=None,
                         otherGifts: Option[Boolean]=None,
                         agriculturalOrBusinessRelief: Option[Boolean]=None,
                         giftsWithReservation: Option[Boolean]=None,
                         benefitFromTrust: Option[Boolean]=None, unusedNilRateBand: Option[BigDecimal]=None)

object SpousesEstate {
  implicit val formats = Json.format[SpousesEstate]
}

case class Spouse(
                   // Person
                   title: Option[String]=None, firstName: Option[String]=None, middleName: Option[String]=None,
                   lastName: Option[String]=None, dateOfBirth: Option[String]=None,
                   gender: Option[String]=None, nino: Option[String]=None, utr: Option[String]=None,
                   mainAddress: Option[Address]=None,

                   // Other
                   dateOfMarriage: Option[String]=None, dateOfDeath: Option[String]=None
                   )

object Spouse {
  implicit val formats = Json.format[Spouse]
}

case class TNRBForm(spouse: Option[Spouse]=None, spousesEstate: Option[SpousesEstate]=None)

object TNRBForm {
  implicit val formats = Json.format[TNRBForm]
}

case class TransferOfNilRateBand(totalNilRateBandTransferred: Option[BigDecimal]=None,
                                 deceasedSpouses: Option[Set[TNRBForm]]=None)

object TransferOfNilRateBand {
  implicit val formats = Json.format[TransferOfNilRateBand]
}

case class SurvivingSpouse(
                            // Person
                            title: Option[String]=None, firstName: Option[String]=None,
                            middleName: Option[String]=None,
                            lastName: Option[String]=None, dateOfBirth: Option[String]=None,
                            gender: Option[String]=None, nino: Option[String]=None, utr: Option[String]=None,
                            mainAddress: Option[Address]=None,

                            // Other
                            dateOfMarriage: Option[String]=None, domicile: Option[String]=None,
                            otherDomicile: Option[String]=None)

object SurvivingSpouse {
  implicit val formats = Json.format[SurvivingSpouse]
}

case class JointOwner(
                       // Person
                       title: Option[String]=None, firstName: Option[String]=None,
                       middleName: Option[String]=None,
                       lastName: Option[String]=None, dateOfBirth: Option[String]=None,
                       gender: Option[String]=None, nino: Option[String]=None, utr: Option[String]=None,
                       mainAddress: Option[Address]=None,

                       // Organisation
                       name: Option[String]=None, ctUtr: Option[String]=None,
                       organisationAddress: Option[Address]=None,

                       // Charity
                       charityNumber: Option[String]=None, charityName: Option[String]=None,
                       charityCountry: Option[String]=None,

                       // Other
                       relationshipToDeceased: Option[String]=None,
                       percentageContribution: Option[String]=None, percentageOwned: Option[String]=None
                       )

object JointOwner {
  implicit val formats = Json.format[JointOwner]
}

case class JointOwnership(percentageOwned: Option[String]=None,
                          jointOwners: Option[Set[JointOwner]]=None,
                          dateOfJointOwnership: Option[String]=None,
                          percentageContribution: Option[String]=None,
                          valueOfShare: Option[String]=None)

object JointOwnership {
  implicit val formats = Json.format[JointOwnership]
}

case class Allocation(percentageShare: Option[String]=None, overrideAmount: Option[String]=None)

object Allocation {
  implicit val formats = Json.format[Allocation]
}

case class OtherBeneficiary(
                             // Person
                             title: Option[String]=None, firstName: Option[String]=None,
                             middleName: Option[String]=None,
                             lastName: Option[String]=None, dateOfBirth: Option[String]=None,
                             gender: Option[String]=None, nino: Option[String]=None, utr: Option[String]=None,
                             mainAddress: Option[Address]=None,

                             // Organisation
                             name: Option[String]=None, ctUtr: Option[String]=None,
                             organisationAddress: Option[Address]=None,

                             // Charity
                             charityNumber: Option[String]=None, charityName: Option[String]=None,
                             charityCountry: Option[String]=None,

                             // Gift for national purpose
                             name1: Option[String]=None
                             )

object OtherBeneficiary {
  implicit val formats = Json.format[OtherBeneficiary]
}

case class Beneficiary(passingToSpouse: Option[String]=None,
                       otherBeneficiary: Option[OtherBeneficiary]=None)

object Beneficiary {
  implicit val formats = Json.format[Beneficiary]
}


case class Exemption(exemptionType: Option[String]=None,
                     percentageAmount: Option[String]=None,
                     overrideValue: Option[BigDecimal]=None)

object Exemption {
  implicit val formats = Json.format[Exemption]
}

case class Devolution(allocation: Option[Allocation]=None,
                      beneficiary: Option[Beneficiary]=None,
                      exemption: Option[Exemption]=None)

object Devolution {
  implicit val formats = Json.format[Devolution]
}

case class Liability(liabilityType: Option[String]=None,
                     liabilityAmount: Option[BigDecimal]=None,
                     liabilityOwner: Option[String]=None)

object Liability {
  implicit val formats = Json.format[Liability]
}

case class Asset(
                  // General asset
                  assetCode: Option[String]=None,
                  assetDescription: Option[String]=None,
                  assetID: Option[String]=None,
                  assetTotalValue: Option[BigDecimal]=None,
                  howheld: Option[String]=None,
                  devolutions: Option[Set[Devolution]]=None,
                  liabilities: Option[Set[Liability]]=None,

                  // Property asset
                  propertyAddress: Option[Address]=None,
                  tenure: Option[String]=None, tenancyType: Option[String]=None,
                  yearsLeftOnLease: Option[Int]=None,
                  yearsLeftOntenancyAgreement: Option[Int]=None,
                  professionalValuation: Option[Boolean]=None
                  )

object Asset {
  implicit val formats = Json.format[Asset]
}

case class Trustee(
                    // Person
                    title: Option[String]=None, firstName: Option[String]=None, middleName: Option[String]=None,
                    lastName: Option[String]=None, dateOfBirth: Option[String]=None,
                    gender: Option[String]=None, nino: Option[String]=None, utr: Option[String]=None,
                    mainAddress: Option[Address]=None,

                    // Organisation
                    name: Option[String]=None, ctUtr: Option[String]=None,
                    organisationAddress: Option[Address]=None
                    )

object Trustee {
  implicit val formats = Json.format[Trustee]
}

case class Submitter(submitterRole: Option[String]=None)

object Submitter {
  implicit val formats = Json.format[Submitter]
}

case class Gift(
                 // General asset
                 assetCode: Option[String]=None,
                 assetDescription: Option[String]=None,
                 assetID: Option[String]=None,
                 assetTotalValue: Option[BigDecimal]=None,
                 howheld: Option[String]=None,
                 devolutions: Option[Set[Devolution]]=None,
                 liabilities: Option[Set[Liability]]=None,

                 // Property asset
                 propertyAddress: Option[Address]=None,
                 tenure: Option[String]=None,
                 tenancyType: Option[String]=None,
                 yearsLeftOnLease: Option[Int]=None,
                 yearsLeftOntenancyAgreement: Option[Int]=None,
                 professionalValuation: Option[Boolean]=None,
                 voaValue: Option[String]=None,
                 jointOwnership: Option[JointOwnership]=None,

                 // Other
                 valuePrevOwned: Option[String]=None,
                 percentageSharePrevOwned: Option[String]=None,
                 valueRetained: Option[String]=None,
                 percentageRetained: Option[String]=None,
                 lossToEstate: Option[String]=None,
                 dateOfGift: Option[String]=None
                 )

object Gift {
  implicit val formats = Json.format[Gift]
}

case class Trust(trustName: Option[String]=None,
                 trustUtr: Option[String]=None,
                 trustees: Option[Set[Trustee]]=None,
                 trustAssets: Option[Set[Asset]]=None,
                 trustLiabilities: Option[Set[Liability]]=None,
                 trustExemptions: Option[Set[Exemption]]=None)

object Trust {
  implicit val formats = Json.format[Trust]
}

case class Declaration(reasonForBeingBelowLimit: Option[String]=None,
                       declarationAccepted: Option[Boolean]=None,
                       coExecutorsAccepted: Option[Boolean]=None,
                       declarationDate: Option[String]=None)

object Declaration {
  implicit val formats = Json.format[Declaration]
}

case class FreeEstate(estateAssets: Option[Set[Asset]]=None)

object FreeEstate {
  implicit val formats = Json.format[FreeEstate]
}

case class Deceased(
                     title: Option[String]=None, firstName: Option[String]=None,
                     middleName: Option[String]=None,
                     lastName: Option[String]=None, dateOfBirth: Option[String]=None,
                     gender: Option[String]=None, nino: Option[String]=None, utr: Option[String]=None,
                     personId:Option[String],
                     mainAddress: Option[Address]=None,

                     dateOfDeath: Option[String]=None, domicile: Option[String]=None,
                     otherDomicile: Option[String]=None, occupation: Option[String]=None,
                     maritalStatus: Option[String]
                     )

object Deceased {
  implicit val formats = Json.format[Deceased]
}

case class Executor(title: Option[String]=None, firstName: Option[String]=None, middleName: Option[String]=None,
                    lastName: Option[String]=None, dateOfBirth: Option[String]=None,
                    gender: Option[String]=None, nino: Option[String]=None, utr: Option[String]=None,
                    personId:Option[String],
                    mainAddress: Option[Address]=None)

object Executor {
  implicit val formats = Json.format[Executor]
}

case class LatestReturn(acknowledgementReference:Option[String], freeEstate:Option[FreeEstate])

object LatestReturn {
  implicit val formats = Json.format[LatestReturn]
}

case class RiskInput(acknowledgementReference: Option[String]=None,
                     riskConsidered:Option[Boolean],
                     eventType:Option[String],
                     entryType:Option[String],
                     deceased:Option[Deceased],
                     latestReturn:Option[LatestReturn])


case class Rule(ruleID: Option[String],
               ruleDescription: Option[String],
               ruleScore: Option[Int],
               ruleCategory: Option[String],
               inTranscationRule: Option[Boolean],
               supportingInformation: Option[String],
               personNino: Option[String],
               personfirstName: Option[String],
               personLastName: Option[String],
               personID: Option[String],
               ihtReference: Option[String],
               assetCode: Option[String],
               assetDescription: Option[String],
               assetID: Option[String]
                 )

object Rule {
  implicit val formats = Json.format[Rule]
}

case class RiskResponse(acknowledgementReference: Option[String],
                        ihtReference: Option[String],
                        assessmentDateTime: Option[String],
                       rulesFired:Option[Set[Rule]]
                         )
object RiskResponse {
  implicit val formats = Json.format[RiskResponse]

}

object RiskInput {
  implicit val formats = Json.format[RiskInput]

  private def buildLatestReturn = {
    val asset = createAsset("9001", BigDecimal(0))
    LatestReturn(
      acknowledgementReference=None,
      freeEstate=
        Some(FreeEstate(estateAssets=Some(Set(asset))))
    )
  }

  def fromRegistrationDetails(rd:RegistrationDetails,
                              acknowledgementReference: String) :RiskInput = {
    RiskInput(
      acknowledgementReference=Some(acknowledgementReference),
      riskConsidered=None,
      eventType=Some(Constants.RiskInputEventType),
      entryType=Some(Constants.RiskInputEntryType),
      deceased=buildDeceased(rd),
      latestReturn=Some(buildLatestReturn)
    )
  }

  private def createAsset(assetCode:String, totalValue:BigDecimal):Asset = {
    Asset(
      assetCode= Some(assetCode),
      assetDescription= AssetDetails.IHTReturnAssetDescription.get(assetCode) ,
      assetID= Some("null"),
      assetTotalValue= Some(totalValue),
      howheld=None)
  }

  private def buildDeceased(rd:RegistrationDetails) = {
    val dateOfDeath:Option[String] =
      for(d<-rd.deceasedDateOfDeath) yield utils.CommonHelper.dateToDesString(d.dateOfDeath)
    for(dd<-rd.deceasedDetails) yield {
      val address = Address(
        Some(dd.ukAddress.ukAddressLine1),
        Some(dd.ukAddress.ukAddressLine2),
        dd.ukAddress.ukAddressLine3,
        dd.ukAddress.ukAddressLine4,
        Some(dd.ukAddress.postCode),
        Some("GB"))

      Deceased(
          title= None,
          firstName= Some(dd.firstName),
          middleName= None,
          lastName= Some(dd.lastName),
          dateOfBirth= Some(utils.CommonHelper.dateToDesString(dd.dateOfBirth)),
          gender= None,
          nino= if (dd.nino.length==0) None else Some(dd.nino),
          utr= None,
          personId=None,
          mainAddress= Some(address),
          dateOfDeath = dateOfDeath,
          domicile=Some(Constants.IHTReturnDummyDomicile),
          otherDomicile=None,
          occupation=None,
          maritalStatus=Some( getModifiedMaritalStatus(dd.maritalStatus) )
      )
    }
  }

  // Get the modified marital status to get aligned with EDH schema. To be removed
  // once EDH schema is fixed at EDH side
  private def getModifiedMaritalStatus(inputMaritalStatus: String): String = {
     Constants.modifiedMaritalStatusForEdh(inputMaritalStatus)
  }
}
