/*
 * Copyright 2019 HM Revenue & Customs
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

import models.application.exemptions.PartnerExemption
import org.joda.time.LocalDate
import uk.gov.hmrc.play.test.UnitSpec
import utils.{CommonBuilder, CommonHelper}
import utils.CommonHelper._

class IHTReturnTest  extends UnitSpec {
  val dateOfDeath = new LocalDate(2000,6,28)
  import org.joda.time.LocalDateTime

  "IHTReturn" should {
    val declarationDate = new LocalDateTime
    val acknowledgmentReference = ""

    "throw an exception if asked to convert an IHT model with a status of kickout" in {
      a [java.lang.RuntimeException] should be thrownBy {
        IHTReturn.fromApplicationDetails(CommonBuilder.buildApplicationDetailsAppStatusKickout,
          declarationDate, acknowledgmentReference, dateOfDeath)
      }
    }

    "successfully convert declaration reasonForBeingBelowLimit to  Excepted Estate" in {
      val ir2 = IHTReturn.fromApplicationDetails(
        CommonBuilder.buildApplicationDetailsReasonForBeingBelowLimitExceptedEstate,
        declarationDate,
        acknowledgmentReference, dateOfDeath)

      val reason:String = (for(d<-ir2.declaration) yield d.reasonForBeingBelowLimit.getOrElse("")).getOrElse("")
      reason shouldBe ("Excepted Estate")
    }

    "successfully convert declaration reasonForBeingBelowLimit to  Spouse, Civil Partner or Charity Exemption" in {
      val ir2 = IHTReturn.fromApplicationDetails(
        CommonBuilder.buildApplicationDetailsReasonForBeingBelowLimitSpouseCivilPartnerCharity,
        declarationDate,
        acknowledgmentReference, dateOfDeath)
      val reason:String = (for(d<-ir2.declaration) yield d.reasonForBeingBelowLimit.getOrElse("")).getOrElse("")
      reason shouldBe ("Spouse, Civil Partner or Charity Exemption")
    }

    "successfully convert declaration reasonForBeingBelowLimit to Transferred Nil Rate Band" in {
      val ir2 = IHTReturn.fromApplicationDetails(
        CommonBuilder.buildApplicationDetailsReasonForBeingBelowLimitTNRB,
        declarationDate,
        acknowledgmentReference, dateOfDeath)
      val reason:String = (for(d<-ir2.declaration) yield d.reasonForBeingBelowLimit.getOrElse("")).getOrElse("")
      reason shouldBe ("Transferred Nil Rate Band")
    }

    "throw an exception if asked to convert an IHT model with a property with an invalid property type" in {
      a [java.lang.RuntimeException] should be thrownBy {
        IHTReturn.fromApplicationDetails(
          CommonBuilder.buildApplicationDetailsAllFields copy(
            propertyList=CommonBuilder.buildPropertyListInvalidPropertyType),
          declarationDate,
          acknowledgmentReference, dateOfDeath)
      }
    }

    "successfully convert a valid IHT model to a valid DES model" in {
      val ir1 = buildIHTReturnCorrespondingToApplicationDetailsAllFields(declarationDate, acknowledgmentReference)
      val ir2 = IHTReturn.fromApplicationDetails(
        CommonBuilder.buildApplicationDetailsAllFields,
        declarationDate,
        acknowledgmentReference, dateOfDeath)

      ir1.submitter shouldBe ir2.submitter
      ir1.declaration shouldBe ir2.declaration
      ir1.deceased shouldBe ir2.deceased
      getOrException(ir1.freeEstate).estateAssets.map( _.toSet ) shouldBe getOrException(ir2.freeEstate).estateAssets.map( _.toSet )
      getOrException(ir1.freeEstate).interestInOtherEstate shouldBe getOrException(ir2.freeEstate).interestInOtherEstate
      getOrException(ir1.freeEstate).estateLiabilities.map( _.toSet ) shouldBe getOrException(ir2.freeEstate).estateLiabilities.map( _.toSet )
      getOrException(ir1.freeEstate).estateExemptions.map( _.toSet ) shouldBe getOrException(ir2.freeEstate).estateExemptions.map( _.toSet )
      ir1.gifts shouldBe ir2.gifts
      ir1.trusts shouldBe ir2.trusts
    }

    "successfully convert a valid IHT model to a valid DES model without Surviving spouse " +
      "when there is no partner exemption " in {

      val freeEstate = FreeEstate(
        estateAssets = Some(buildAssets),
        interestInOtherEstate = None,
        estateLiabilities = Some(buildLiabilities),
        estateExemptions = Some(buildExemptionsWithoutSpouse)
      )

      val ir1 = buildIHTReturnCorrespondingToApplicationDetailsAllFields(declarationDate, acknowledgmentReference).copy(
        deceased = Some(buildTNRB.copy(survivingSpouse = None)),
        freeEstate = Some(freeEstate)
      )

      val allExemptions = Some(CommonBuilder.buildAllExemptions.copy(
        partner = Some(PartnerExemption(Some(false), None, None, None, None, None, None))))

      val ir2 = IHTReturn.fromApplicationDetails(
        CommonBuilder.buildApplicationDetailsAllFields.copy(allExemptions = allExemptions),
        declarationDate,
        acknowledgmentReference,
        dateOfDeath)

      ir1.submitter shouldBe ir2.submitter
      ir1.declaration shouldBe ir2.declaration
      ir1.deceased shouldBe ir2.deceased
      getOrException(ir1.freeEstate).estateAssets.map(_.toSet) shouldBe getOrException(ir2.freeEstate).estateAssets.map(_.toSet)
      getOrException(ir1.freeEstate).interestInOtherEstate shouldBe getOrException(ir2.freeEstate).interestInOtherEstate
      getOrException(ir1.freeEstate).estateLiabilities.map(_.toSet) shouldBe getOrException(ir2.freeEstate).estateLiabilities.map(_.toSet)
      getOrException(ir1.freeEstate).estateExemptions.map(_.toSet) shouldBe getOrException(ir2.freeEstate).estateExemptions.map(_.toSet)
      ir1.gifts shouldBe ir2.gifts
      ir1.trusts shouldBe ir2.trusts
    }
  }

  def buildIHTReturnCorrespondingToApplicationDetailsAllFields(declarationDate:LocalDateTime,
                                                               acknowledgmentReference: String) = {
    val address = Address(
      addressLine1= Some("addr1"), addressLine2= Some("addr2"),
      addressLine3= None, addressLine4= None,
      postalCode= Some("AA1 1AA"), countryCode= Some("GB")
    )

    val declaration = Declaration(
      reasonForBeingBelowLimit= Some("Excepted Estate"),
      declarationAccepted= Some(true),
      coExecutorsAccepted= Some(true),
      declarationDate= Some(utils.CommonHelper.dateTimeToDesString(declarationDate)))

    val freeEstate = FreeEstate(
      estateAssets= Some(buildAssets),
      interestInOtherEstate= None,
      estateLiabilities= Some(buildLiabilities),
      estateExemptions= Some(buildExemptions)
    )

    IHTReturn(Some(acknowledgmentReference),
      submitter=Some(Submitter(submitterRole=Some("Lead Executor"))),
      deceased=Some(buildTNRB),
      freeEstate=Some(freeEstate),
      gifts=Some(buildGifts),
      trusts=Some(buildTrusts),
      declaration=Some(declaration))
  }

  private def buildGifts = {
    Set(Set(
      makeGiftWithOutExemption(1000, "2005-04-05"),
      makeGiftWithExemption(2000, 200, "2006-04-05"),
      makeGiftWithOutExemption(3000, "2007-04-05"),
      makeGiftWithOutExemption(4000, "2008-04-05"),
      makeGiftWithOutExemption(5000, "2009-04-05"),
      makeGiftWithOutExemption(6000, "2010-04-05"),
      makeGiftWithOutExemption(7000, "2011-04-05")
    ))
  }

  private def buildTrusts = {
    Set(
      makeTrust(17)
    )
  }

  private def makeTrust(value:BigDecimal) = {
    Trust(
      trustName=Some("Deceased Trust"),
      trustAssets=
        Some(
          Set(
            Asset(
              // General asset
              assetCode= Some("9097"),
              assetDescription= Some("Rolled up trust assets"),
              assetID= Some("null"),
              assetTotalValue= Some(value),
              howheld= Some("Standard"),
              devolutions= None,
              liabilities= None
      )))
    )
  }

  private def makeGiftWithOutExemption(value:BigDecimal, date:String) = {
    Gift(
      assetCode=Some("9095"),
      assetDescription=Some("Rolled up gifts"),
      assetID=Some("null"),
      valuePrevOwned = Some(value),
      percentageSharePrevOwned = Some(BigDecimal(100)),
      valueRetained = Some(BigDecimal(0)),
      percentageRetained = Some(BigDecimal(0)),
      lossToEstate = Some(value),
      dateOfGift = Some(date),
      assetTotalValue = Some(value),
      howheld = Some("Standard")
    )
  }

  private def makeGiftWithExemption(value: BigDecimal, exemptions: BigDecimal, date: String) = {
    val totalGiftValue = value - exemptions
    Gift(
      assetCode=Some("9095"),
      assetDescription=Some(s"Rolled up gifts minus exemption of £$exemptions"),
      assetID=Some("null"),
      valuePrevOwned = Some(value),
      percentageSharePrevOwned = Some(BigDecimal(100)),
      valueRetained = Some(BigDecimal(0)),
      percentageRetained = Some(BigDecimal(0)),
      lossToEstate = Some(totalGiftValue),
      dateOfGift = Some(date),
      assetTotalValue = Some(totalGiftValue),
      howheld = Some("Standard")
    )
  }

  private def makeGiftToTrustOrganisation(value:BigDecimal, date:String) = {
    Gift(
      assetCode=Some("9095"),
      assetDescription=Some("Rolled up gifts given away to Trusts or Organisation"),
      assetID=Some("null"),
      valuePrevOwned = Some(value),
      percentageSharePrevOwned = Some(BigDecimal(100)),
      valueRetained = Some(BigDecimal(0)),
      percentageRetained = Some(BigDecimal(0)),
      lossToEstate = Some(value),
      dateOfGift = Some(date),
      assetTotalValue = Some(value),
      howheld = Some("Standard")
    )
  }

  private def buildTNRB = {
    import constants.Constants
    val survivingSpouse = SurvivingSpouse(
      // Person
      title= None, firstName= Some("ABCDE"), middleName= None,
      lastName= Some("XYZAB"), dateOfBirth= Some("2011-11-12"),
      gender= None, nino= None, utr= None,
      mainAddress= None,

      // Other
      dateOfMarriage= Some(CommonHelper.dateToDesString(dateOfDeath.minusDays(1))), domicile= Some(Constants.IHTReturnDummyDomicile),
      otherDomicile= None)

    val spouse = Spouse(
      // Person
      title= None, firstName= Some("ABCXYZ"), middleName= None,
      lastName= Some("XYZABC"), dateOfBirth= Some(Constants.IHTReturnDummyDateOfBirth),
      gender= None, nino= None, utr= None,
      mainAddress= None,

      // Other
      dateOfMarriage= Some("2008-12-13"), dateOfDeath=Some("2010-10-12")
    )

    val spousesEstate = SpousesEstate(
      domiciledInUk= Some(true), whollyExempt= Some(false), jointAssetsPassingToOther= Some(true),
      otherGifts= Some(false), agriculturalOrBusinessRelief= Some(true), giftsWithReservation= Some(false),
      benefitFromTrust= Some(true), unusedNilRateBand= Some(BigDecimal(100))
    )

    val tnrbForm = TNRBForm(
      spouse=Some(spouse),
      spousesEstate=Some(spousesEstate)
    )

    Deceased(survivingSpouse=Some(survivingSpouse),
      transferOfNilRateBand=Some(TransferOfNilRateBand(
        totalNilRateBandTransferred=Some(BigDecimal(100)),
        deceasedSpouses = Some(Set(tnrbForm))))
    )
  }



  // Liabilities excluding mortgages, split into funeral expenses and other.
  private def buildLiabilities = {
    import constants.Constants
    val liabilityFuneralExp = Liability(
      liabilityType=Some("Funeral Expenses"),
      liabilityAmount=Some(BigDecimal(20)),
      liabilityOwner=Some(Constants.IHTReturnDummyLiabilityOwner)
    )

    val liabilityOther = Liability(
      liabilityType=Some("Other"),
      liabilityAmount=Some(BigDecimal(90)),
      liabilityOwner=Some(Constants.IHTReturnDummyLiabilityOwner)
    )

    Seq(liabilityFuneralExp, liabilityOther)
  }

  // Not sure whether to include spouse exemption (already included in surviving spouse section).
  private def buildExemptions = {
    val exemption1 = Exemption(
      exemptionType=Some("Charity"),
      percentageAmount=None,
      overrideValue=Some(BigDecimal(27))
    )
    val exemption2 = Exemption(
      exemptionType=Some("Charity"),
      percentageAmount=None,
      overrideValue=Some(BigDecimal(28))
    )
    val exemption3 = Exemption(
      exemptionType=Some("GNCP"),
      percentageAmount=None,
      overrideValue=Some(BigDecimal(30))
    )
    val exemption4 = Exemption(
      exemptionType=Some("GNCP"),
      percentageAmount=None,
      overrideValue=Some(BigDecimal(31))
    )
    val exemption5 = Exemption(
      exemptionType=Some("Spouse"),
      percentageAmount=None,
      overrideValue=Some(BigDecimal(25))
    )
    Seq(exemption1, exemption2, exemption3, exemption4, exemption5)
  }

  private def buildExemptionsWithoutSpouse = {
    val exemption1 = Exemption(
      exemptionType=Some("Charity"),
      percentageAmount=None,
      overrideValue=Some(BigDecimal(27))
    )
    val exemption2 = Exemption(
      exemptionType=Some("Charity"),
      percentageAmount=None,
      overrideValue=Some(BigDecimal(28))
    )
    val exemption3 = Exemption(
      exemptionType=Some("GNCP"),
      percentageAmount=None,
      overrideValue=Some(BigDecimal(30))
    )
    val exemption4 = Exemption(
      exemptionType=Some("GNCP"),
      percentageAmount=None,
      overrideValue=Some(BigDecimal(31))
    )
    Seq(exemption1, exemption2, exemption3, exemption4)
  }

  private def buildAssets = {
    Seq(
      buildAssetMoney,
      buildJointAssetMoney,
      buildAssetHouseholdAndPersonalItems,
      buildJointAssetHouseholdAndPersonalItems,
      buildAssetStocksAndSharesListed,
      buildAssetStocksAndSharesNotListed,
      buildAssetPrivatePensions,
      buildAssetInsurancePoliciesOwned,
      buildJointAssetInsurancePoliciesOwned,
      buildAssetBusinessInterests,
      buildAssetNominatedAssets,
      buildAssetForeignAssets,
      buildAssetMoneyOwed,
      buildAssetOther,
      buildAssetsPropertiesDeceasedsHome,
      buildAssetsPropertiesOtherResidentialBuilding,
      buildAssetsPropertiesLandNonRes
    )
  }

  private def buildAssetMoney = {
    Asset(
      // General asset
      assetCode= Some("9001"),
      assetDescription= Some("Rolled up bank and building society accounts"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(1)),
      howheld= Some("Standard"),
      devolutions= None,
      liabilities= None
    )
  }

  // Create Jointly owed money asset
  private def buildJointAssetMoney = {
    Asset(
      // General asset
      assetCode= Some("9001"),
      assetDescription= Some("Rolled up bank and building society accounts"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(2)),
      howheld= Some("Joint - Beneficial Joint Tenants"),
      devolutions= None,
      liabilities= None
    )
  }

  // Household and personal goods plus motor vehicles, caravans and boats
  private def buildAssetHouseholdAndPersonalItems = {
    Asset(
      // General asset
      assetCode= Some("9004"),
      assetDescription= Some("Rolled up household and personal goods"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(8)),
      howheld= Some("Standard"),
      devolutions= None,
      liabilities= None
    )
  }

  // Create joint household and personal items
  private def buildJointAssetHouseholdAndPersonalItems = {
    Asset(
      // General asset
      assetCode= Some("9004"),
      assetDescription= Some("Rolled up household and personal goods"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(10)),
      howheld= Some("Joint - Beneficial Joint Tenants"),
      devolutions= None,
      liabilities= None
    )
  }

  // Create joint household and personal items
  private def buildJointAssetMotorVehicle = {
    Asset(
      // General asset
      assetCode= Some("9004"),
      assetDescription= Some("Rolled up household and personal goods"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(6)),
      howheld= Some("Joint - Beneficial Joint Tenants"),
      devolutions= None,
      liabilities= None
    )
  }

  private def buildAssetPrivatePensions = {
    Asset(
      // General asset
      assetCode= Some("9005"),
      assetDescription= Some("Rolled up pensions"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(7)),
      howheld= Some("Standard"),
      devolutions= None,
      liabilities= None
    )
  }

  // Create jointly owen private pensions
  private def buildJointAssetPrivatePensions = {
    Asset(
      // General asset
      assetCode= Some("9005"),
      assetDescription= Some("Rolled up pensions"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(8)),
      howheld= Some("Joint - Beneficial Joint Tenants"),
      devolutions= None,
      liabilities= None
    )
  }

  private def buildAssetStocksAndSharesNotListed = {
    Asset(
      // General asset
      assetCode= Some("9010"),
      assetDescription= Some("Rolled up unlisted stocks and shares"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(9)),
      howheld= Some("Standard"),
      devolutions= None,
      liabilities= None
    )
  }

  private def buildAssetStocksAndSharesListed = {
    Asset(
      // General asset
      assetCode= Some("9008"),
      assetDescription= Some("Rolled up quoted stocks and shares"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(10)),
      howheld= Some("Standard"),
      devolutions= None,
      liabilities= None
    )
  }

  private def buildAssetInsurancePoliciesOwned = {
    Asset(
      // General asset
      assetCode= Some("9006"),
      assetDescription= Some("Rolled up life assurance policies"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(12)),
      howheld= Some("Standard"),
      devolutions= None,
      liabilities= None
    )
  }

  // Create jointly owed insurance policy
  private def buildJointAssetInsurancePoliciesOwned = {
    Asset(
      // General asset
      assetCode= Some("9006"),
      assetDescription= Some("Rolled up life assurance policies"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(13)),
      howheld= Some("Joint - Beneficial Joint Tenants"),
      devolutions= None,
      liabilities= None
    )
  }

  private def buildAssetBusinessInterests = {
    Asset(
      // General asset
      assetCode= Some("9021"),
      assetDescription= Some("Rolled up business assets"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(14)),
      howheld= Some("Standard"),
      devolutions= None,
      liabilities= None
    )
  }

  private def buildAssetNominatedAssets = {

    Asset(
      // General asset
      assetCode= Some("9099"),
      assetDescription= Some("Rolled up nominated assets"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(16)),
      howheld= Some("Nominated"),
      devolutions= None,
      liabilities= None
    )
  }

  private def buildAssetForeignAssets = {
    Asset(
      // General asset
      assetCode= Some("9098"),
      assetDescription= Some("Rolled up foreign assets"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(18)),
      howheld= Some("Foreign"),
      devolutions= None,
      liabilities= None
    )
  }

  private def buildAssetMoneyOwed = {
    Asset(
      // General asset
      assetCode= Some("9013"),
      assetDescription= Some("Rolled up money owed to deceased"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(15)),
      howheld= Some("Standard"),
      devolutions= None,
      liabilities= None
    )
  }

  private def buildAssetOther = {
    Asset(
      // General asset
      assetCode= Some("9015"),
      assetDescription= Some("Rolled up other assets"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(19)),
      howheld= Some("Standard"),
      devolutions= None,
      liabilities= None
    )
  }

  private def buildAssetsPropertiesDeceasedsHome = {
    import constants.Constants
    val addressOrOtherLandLocation = AddressOrOtherLandLocation(
        addressLine1= Some("addr1"), addressLine2= Some("addr2"),
        addressLine3= None, addressLine4= None,
        postalCode= Some("AA1 1AA"), countryCode= Some("GB")
    )

    val liability1 = Liability(
      liabilityType=Some("Mortgage"),
      liabilityAmount=Some(BigDecimal(80)),
      liabilityOwner=Some(Constants.IHTReturnDummyLiabilityOwner)
    )

    Asset(
      // General asset
      assetCode= Some("0016"),
      assetDescription= Some("Deceased's residence"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(100)),
      howheld= Some("Standard"),
      liabilities= Some(Set(liability1)),
//      liabilities= None,

      // Property asset
      propertyAddress= Some(addressOrOtherLandLocation),
      tenure= Some("Freehold"), tenancyType= Some("Vacant Possession"),
      yearsLeftOnLease= Some(0),
      yearsLeftOntenancyAgreement= Some(0)
    )
  }

  private def buildAssetsPropertiesOtherResidentialBuilding = {
    import constants.Constants
    val addressOrOtherLandLocation = AddressOrOtherLandLocation(
        addressLine1= Some("addr1"), addressLine2= Some("addr2"),
        addressLine3= None, addressLine4= None,
        postalCode= Some("AA1 1AA"), countryCode= Some("GB"))

    val liability1 = Liability(
      liabilityType=Some("Mortgage"),
      liabilityAmount=Some(BigDecimal(150)),
      liabilityOwner=Some(Constants.IHTReturnDummyLiabilityOwner)
    )

    Asset(
      // General asset
      assetCode= Some("0017"),
      assetDescription= Some("Other residential property"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(200)),
      howheld= Some("Joint - Beneficial Joint Tenants"),
      devolutions= None,
      liabilities= Some(Set(liability1)),
//      liabilities= None,

      // Property asset
      propertyAddress= Some(addressOrOtherLandLocation),
      tenure= Some("Leasehold"), tenancyType= Some("Vacant Possession"),
      yearsLeftOnLease= Some(0),
      yearsLeftOntenancyAgreement= Some(0)
    )
  }

  private def buildAssetsPropertiesLandNonRes = {
    val addressOrOtherLandLocation = AddressOrOtherLandLocation(
        addressLine1= Some("addr1"), addressLine2= Some("addr2"),
        addressLine3= None, addressLine4= None,
        postalCode= Some("AA1 1AA"), countryCode= Some("GB"))

    Asset(
      // General asset
      assetCode= Some("0018"),
      assetDescription= Some("Other land and buildings"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(300)),
      howheld= Some("Joint - Tenants In Common"),
      devolutions= None,

      // Property asset
      propertyAddress= Some(addressOrOtherLandLocation),
      tenure= Some("Leasehold"), tenancyType= Some("Vacant Possession"),
      yearsLeftOnLease= Some(0),
      yearsLeftOntenancyAgreement= Some(0)
    )
  }
}
