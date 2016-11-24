/*
 * Copyright 2016 HM Revenue & Customs
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

package models.des

import models.ApplicationDetails
import play.api.libs.json.Json
import org.joda.time.LocalDate
import constants.Constants
import org.joda.time.LocalDateTime
import utils.des.IHTReturnHelper

case class Address(addressLine1: Option[String]=None, addressLine2: Option[String]=None,
                   addressLine3: Option[String], addressLine4: Option[String],
                   postalCode: Option[String]=None, countryCode: Option[String]=None)

object Address {
  implicit val formats = Json.format[Address]
}

case class OtherAddress(addressLine1: Option[String]=None, addressLine2: Option[String]=None,
                        addressLine3: Option[Option[String]] = None, addressLine4: Option[Option[String]] = None,
                        postalCode: Option[String]=None, countryCode: Option[String]=None,
                        addressType: Option[String]=None)

object OtherAddress {
  implicit val formats = Json.format[OtherAddress]
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
                   mainAddress: Option[Address]=None, OtherAddresses: Option[Set[OtherAddress]]=None,

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
                            mainAddress: Option[Address]=None, OtherAddresses: Option[Set[OtherAddress]]=None,

                            // Other
                            dateOfMarriage: Option[String]=None, domicile: Option[String]=None,
                            otherDomicile: Option[String]=None)

object SurvivingSpouse {
  implicit val formats = Json.format[SurvivingSpouse]
}

case class OtherLandLocation(locationDescription: Option[String]=None)

object OtherLandLocation {
  implicit val formats = Json.format[OtherLandLocation]
}

case class AddressOrOtherLandLocation(addressLine1: Option[String]=None, addressLine2: Option[String]=None,
                                      addressLine3: Option[String], addressLine4: Option[String],
                                      postalCode: Option[String]=None, countryCode: Option[String]=None,
                                      otherLandLocation: Option[OtherLandLocation]=None)

object AddressOrOtherLandLocation {
  implicit val formats = Json.format[AddressOrOtherLandLocation]
}

case class JointOwner(
                       // Person
                       title: Option[String]=None, firstName: Option[String]=None,
                       middleName: Option[String]=None,
                       lastName: Option[String]=None, dateOfBirth: Option[String]=None,
                       gender: Option[String]=None, nino: Option[String]=None, utr: Option[String]=None,
                       mainAddress: Option[Address]=None, OtherAddresses: Option[Set[OtherAddress]]=None,

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
                             mainAddress: Option[Address]=None, OtherAddresses: Option[Set[OtherAddress]]=None,

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
                     percentageAmount: Option[BigDecimal]=None,
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
                  propertyAddress: Option[AddressOrOtherLandLocation]=None,
                  tenure: Option[String]=None, tenancyType: Option[String]=None,
                  yearsLeftOnLease: Option[Int]=None,
                  yearsLeftOntenancyAgreement: Option[Int]=None,
                  professionalValuation: Option[Boolean]=None
                  )

object Asset {
  implicit val formats = Json.format[Asset]
}

case class InterestInOtherEstate(
                  // Person
                  title: Option[String]=None, firstName: Option[String]=None, middleName: Option[String]=None,
                  lastName: Option[String]=None, dateOfBirth: Option[String]=None,
                  gender: Option[String]=None, nino: Option[String]=None, utr: Option[String]=None,
                  mainAddress: Option[Address]=None, OtherAddresses: Option[Set[OtherAddress]]=None,

                  // Other
                  otherEstateAssets: Option[Set[Asset]]=None
                                  )

object InterestInOtherEstate {
  implicit val formats = Json.format[InterestInOtherEstate]
}

case class Trustee(
                    // Person
                    title: Option[String]=None, firstName: Option[String]=None, middleName: Option[String]=None,
                    lastName: Option[String]=None, dateOfBirth: Option[String]=None,
                    gender: Option[String]=None, nino: Option[String]=None, utr: Option[String]=None,
                    mainAddress: Option[Address]=None,
                    OtherAddresses: Option[Set[OtherAddress]]=None,

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

case class Deceased(survivingSpouse: Option[SurvivingSpouse]=None,
                    transferOfNilRateBand: Option[TransferOfNilRateBand]=None)

object Deceased {
  implicit val formats = Json.format[Deceased]
}

case class FreeEstate(estateAssets: Option[Seq[Asset]]=None,
                      interestInOtherEstate: Option[InterestInOtherEstate]=None,
                      estateLiabilities: Option[Seq[Liability]]=None,
                      estateExemptions: Option[Seq[Exemption]]=None)

object FreeEstate {
  implicit val formats = Json.format[FreeEstate]
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
                 propertyAddress: Option[AddressOrOtherLandLocation]=None,
                 tenure: Option[String]=None,
                 tenancyType: Option[String]=None,
                 yearsLeftOnLease: Option[Int]=None,
                 yearsLeftOntenancyAgreement: Option[Int]=None,
                 professionalValuation: Option[Boolean]=None,
                 voaValue: Option[String]=None,
                 jointOwnership: Option[JointOwnership]=None,

                 // Other
                 valuePrevOwned: Option[BigDecimal]=None,
                 percentageSharePrevOwned: Option[BigDecimal]=None,
                 valueRetained: Option[BigDecimal]=None,
                 percentageRetained: Option[BigDecimal]=None,
                 lossToEstate: Option[BigDecimal]=None,
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

case class IHTReturn(acknowledgmentReference: Option[String]=None,
                     submitter: Option[Submitter]=None,
                     deceased: Option[Deceased]=None,
                     freeEstate: Option[FreeEstate]=None,
                     gifts: Option[Set[Set[Gift]]]=None,
                     trusts: Option[Set[Trust]]=None,
                     declaration: Option[Declaration]=None)

object IHTReturn {
  implicit val formats = Json.format[IHTReturn]
  def fromApplicationDetails(ad:ApplicationDetails,
                             declarationDate:LocalDateTime,
                             acknowledgmentReference: String,
                              dateOfDeath:LocalDate) :IHTReturn = {

    val kickoutReason = ad.kickoutReason.getOrElse("")
    if (kickoutReason.length>0) {
      throw new RuntimeException("Application is kicked out with a kickoutReason of " + kickoutReason + ". The application details object is: " + ad.toString())
    }

    IHTReturn(Some(acknowledgmentReference),
      submitter=Some(Submitter(submitterRole=Some(Constants.IHTReturnSubmitterRole))),
      deceased=Some(IHTReturnHelper.buildDeceased(ad, dateOfDeath)),
      freeEstate=IHTReturnHelper.buildFreeEstate(ad),
      gifts=IHTReturnHelper.buildGifts(ad, dateOfDeath),
      trusts=IHTReturnHelper.buildTrusts(ad),
      declaration=Some(IHTReturnHelper.buildDeclaration(ad, declarationDate)))
  }
}
