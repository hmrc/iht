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

package utils.des

import constants.{AssetDetails, Constants}
import models.des._
import models._
import org.joda.time.{LocalDateTime, LocalDate}
import utils.CommonHelper._

import scala.collection.mutable.ListBuffer

/**
 *
 * Created by Vineet Tyagi on 24/11/15.
 *
 */
object IHTReturnHelper {

  /**
   * Create Deceased details
   * @param ad
   * @param dateOfDeath
   * @return
   */
  def buildDeceased(ad:ApplicationDetails, dateOfDeath:LocalDate) = {
    val partnerExemption:Option[models.PartnerExemption] = getPartnerExemption(ad)
    val spouse:Option[Spouse] = for( a<-ad.increaseIhtThreshold ) yield {
      Spouse(
        // Person
        title= None, firstName= a.firstName, middleName= None,
        lastName= a.lastName, dateOfBirth= Some(Constants.IHTReturnDummyDateOfBirth),
        gender= None, nino= None, utr= None,
        mainAddress= None, OtherAddresses= None,

        // Other
        dateOfMarriage= a.dateOfMarriage.flatMap(date=> Some(dateToDesString(date))),
        dateOfDeath = ad.widowCheck.flatMap(widow=>widow.dateOfPreDeceased.flatMap(date=> Some(dateToDesString(date))))
      )
    }

    val spousesEstate:Option[SpousesEstate] = for( a<-ad.increaseIhtThreshold ) yield {
      SpousesEstate(
        domiciledInUk= a.isPartnerLivingInUk,
        whollyExempt=a.isEstateBelowIhtThresholdApplied,
        jointAssetsPassingToOther=a.isJointAssetPassed,
        otherGifts= a.isGiftMadeBeforeDeath,
        agriculturalOrBusinessRelief=a.isStateClaimAnyBusiness,
        giftsWithReservation=a.isPartnerGiftWithResToOther,
        benefitFromTrust=a.isPartnerBenFromTrust, unusedNilRateBand=Some(BigDecimal(100)))
    }
    val transferOfNilRateBand:Option[TransferOfNilRateBand] = if(spouse.isDefined) {
      Some(TransferOfNilRateBand(
        totalNilRateBandTransferred=Some(BigDecimal(100)),
        deceasedSpouses = Some(Set(TNRBForm(spouse=spouse, spousesEstate=spousesEstate)))))
    } else {
      None
    }

    Deceased(survivingSpouse = partnerExemption.map { pe => buildSurvivingSpouse(pe, dateOfDeath)},
      transferOfNilRateBand = transferOfNilRateBand)

  }

  /**
    * Create the Surviving Spouse to be send to Des
    * @param pe
    * @param dateOfDeath
    */
  private def buildSurvivingSpouse(pe: PartnerExemption,
                                   dateOfDeath:LocalDate): SurvivingSpouse = {

      SurvivingSpouse(
        title= None,
        firstName= pe.firstName,
        middleName= None,
        lastName= pe.lastName,
        dateOfBirth = pe.dateOfBirth.map {dob => dateToDesString(dob)},
        gender= None,
        nino= None,
        utr= None,
        mainAddress= None,
        OtherAddresses= None,
        // Other - dateOfMarriage is one day before Deceased date of death
        dateOfMarriage= Some(dateToDesString(dateOfDeath.minusDays(1))),
        domicile= Some(Constants.IHTReturnDummyDomicile), otherDomicile= None)
  }

  /**
   * Create free state
   * @param ad - ApplicationDetails
   * @return
   */
  def buildFreeEstate(ad:ApplicationDetails):Option[FreeEstate] = {
    val assets:Seq[Asset] = buildAssets(ad)
    val exemptions:Seq[Exemption] = buildExemptions(ad)
    val liabilities:Seq[Liability] = buildLiabilities(ad)

    val fe = FreeEstate(
      estateAssets= if(assets.size>0) Some(assets) else None,
      interestInOtherEstate= None,
      estateLiabilities= if(liabilities.size>0) Some(liabilities) else None,
      estateExemptions= if(exemptions.size>0) Some(exemptions) else None
    )
    Some(fe)
  }

  /**
   * Creates the assets for the application
   * @param ad
   * @return
   */
  private def buildAssets(ad:ApplicationDetails) = {
    val assets = new ListBuffer[Asset]()
    // Create assets with property and liabilities
    buildAssetWithPropertyAndLiabilities(ad, assets)
    // Create individual assets
    buildIndividualAsset(ad, assets)
    // Create joint assets
    buildJointAssets(ad, assets)
    assets.toSeq
  }

  /**
   *
   * @param appDetails
   * @param assets
   * @return
   */
   private def buildAssetWithPropertyAndLiabilities(appDetails: ApplicationDetails, assets: ListBuffer[Asset]) = {

    // Add properties assets and for each map any mortgage to liability element
    for(p<-appDetails.propertyList) yield {
      val assetCode = Constants.IHTReturnPropertyAssetCode.get(p.propertyType.getOrElse("")).getOrElse("")
      if (assetCode.length==0) throw new RuntimeException("Property type \"" + p.propertyType + "\" is invalid.")
      val assetDescription = Constants.IHTReturnPropertyAssetDescription.get(p.propertyType.getOrElse("")).getOrElse("")
      val propertyId = p.id.getOrElse("")
      val totalValue = p.value

      val liabilityValue:BigDecimal = (for(l<-appDetails.allLiabilities) yield {
        val mortgageList:Option[List[Mortgage]] = l.mortgages.map( _.mortgageList )
        if (mortgageList.isDefined) {
          getOrException(mortgageList).filter(x => x.id == propertyId).map(_.value.getOrElse(BigDecimal(0))).sum
        } else {
          BigDecimal(0)
        }
      }).getOrElse(BigDecimal(0))

      val liabilities = if (liabilityValue > 0) {
        Some(Set(Liability(
          liabilityType = Some("Mortgage"),
          liabilityAmount = Some(liabilityValue),
          liabilityOwner = Some(Constants.IHTReturnDummyLiabilityOwner))))
      } else {
        None
      }

      assets += Asset(
        assetCode = Some(assetCode),
        assetDescription = Some(assetDescription),
        assetID = Some("null"),
        assetTotalValue = totalValue,
        howheld = Constants.IHTReturnHowHeld.get(p.typeOfOwnership.getOrElse("")),
        propertyAddress = buildAddressOrOtherLandLocation(p),
        tenure = if (p.tenure.getOrElse("").length > 0) Some(p.tenure.getOrElse("")) else None,
        tenancyType = Some("Vacant Possession"),
        yearsLeftOnLease = Some(0),
        yearsLeftOntenancyAgreement = Some(0),
        liabilities = liabilities)
    }
  }

  /**
   *
   * Creates individual asset
   * @param appDetails
   * @param assets
   * @return
   */
  private def buildIndividualAsset(appDetails: ApplicationDetails, assets: ListBuffer[Asset]) ={
    buildAssetMoney(appDetails, assets)
    buildAssetHouseHold(appDetails, assets)
    buildAssetPrivatePension(appDetails, assets)
    buildAssetStockAndShareNotListed(appDetails, assets)
    buildAssetStockAndShareListed(appDetails, assets)
    buildAssetInsurancePolicy(appDetails, assets)
    buildAssetBusinessInterest(appDetails, assets)
    buildAssetNominated(appDetails, assets)
    buildAssetForeign(appDetails, assets)
    buildAssetMoneyOwedToDeceased(appDetails, assets)
    buildAssetOther(appDetails, assets)
  }

  private def buildAssetMoney(appDetails: ApplicationDetails, assets: ListBuffer[Asset])={

    val moneyValue = appDetails.allAssets.flatMap(_.money.flatMap(_.value)).getOrElse(BigDecimal(0))
    for (a <- createAsset(AssetDetails.AssetCodeMoney, moneyValue)) yield {
      assets += a
    }
  }

  private def buildAssetHouseHold(appDetails: ApplicationDetails, assets: ListBuffer[Asset]) = {

    val houseHoldTotalValue = appDetails.allAssets.flatMap(_.household.flatMap(_.value)).getOrElse(BigDecimal(0))
    val vehiclesTotalValue = appDetails.allAssets.flatMap(_.vehicles.flatMap(_.value)).getOrElse(BigDecimal(0))

    val totalHouseHoldsValue = houseHoldTotalValue + vehiclesTotalValue
    for (a <- createAsset(AssetDetails.AssetCodeHouseHold, totalHouseHoldsValue)) yield {
      assets += a
    }
  }

  private def buildAssetPrivatePension(appDetails: ApplicationDetails, assets: ListBuffer[Asset]) = {

    val privatePensionTotalValue = appDetails.allAssets.flatMap(_.privatePension.flatMap(_.value)).getOrElse(BigDecimal(0))
    for (a <- createAsset(AssetDetails.AssetCodePrivatePension, privatePensionTotalValue)) yield {
      assets += a
    }
  }

  private def buildAssetStockAndShareNotListed(appDetails: ApplicationDetails, assets: ListBuffer[Asset]) = {

    val totalStockAndShareNotListedValue = appDetails.allAssets.flatMap(_.stockAndShare.flatMap(_.valueNotListed)).getOrElse(BigDecimal(0))
    for (a <- createAsset(AssetDetails.AssetCodeStockShareNotListed, totalStockAndShareNotListedValue)) yield {
      assets += a
    }
  }

  private def buildAssetStockAndShareListed(appDetails: ApplicationDetails, assets: ListBuffer[Asset]) = {
    val totalStockAndShareListedValue = appDetails.allAssets.flatMap(_.stockAndShare.flatMap(_.valueListed)).getOrElse(BigDecimal(0))
    for (a <- createAsset(AssetDetails.AssetCodeStockShareListed, totalStockAndShareListedValue)) yield {
      assets += a
    }
  }

  private def buildAssetInsurancePolicy(appDetails: ApplicationDetails, assets: ListBuffer[Asset]) = {

    val totalInsurancePolicyValue = appDetails.allAssets.flatMap(_.insurancePolicy.flatMap(_.value)).getOrElse(BigDecimal(0))
    for (a <- createAsset(AssetDetails.AssetCodeInsurancePolicy, totalInsurancePolicyValue)) yield {
      assets += a
    }
  }
  private def buildAssetBusinessInterest(appDetails: ApplicationDetails, assets: ListBuffer[Asset]) = {
    val businessInterestTotalValue = appDetails.allAssets.flatMap(_.businessInterest.flatMap(_.value)).getOrElse(BigDecimal(0))
    for (a <- createAsset(AssetDetails.AssetCodeBusinessInterest, businessInterestTotalValue)) yield {
      assets += a
    }
  }

  private def buildAssetNominated(appDetails: ApplicationDetails, assets: ListBuffer[Asset]) = {

    val nominatedAssetTotalValue = appDetails.allAssets.flatMap(_.nominated.flatMap(_.value)).getOrElse(BigDecimal(0))
    for (a <- createAsset(AssetDetails.AssetCodeNominatedAsset, nominatedAssetTotalValue, Constants.howHeldNominated)) yield {
      assets += a
    }
  }

  private def buildAssetForeign(appDetails: ApplicationDetails, assets: ListBuffer[Asset]) = {

    val foreignAssetTotalValue = appDetails.allAssets.flatMap(_.foreign.flatMap(_.value)).getOrElse(BigDecimal(0))
    for (a <- createAsset(AssetDetails.AssetCodeForeignAsset, foreignAssetTotalValue, Constants.howHeldForeign)) yield {
      assets += a
    }
  }

  private def buildAssetMoneyOwedToDeceased(appDetails: ApplicationDetails, assets: ListBuffer[Asset]) = {
    val moneyOwedToDeceasedTotalValue = appDetails.allAssets.flatMap(_.moneyOwed.flatMap(_.value)).getOrElse(BigDecimal(0))
    for (a <- createAsset(AssetDetails.AssetCodeMoneyOwed, moneyOwedToDeceasedTotalValue)) yield {
      assets += a
    }
  }

  private def buildAssetOther(appDetails: ApplicationDetails, assets: ListBuffer[Asset]) = {
    val otherAssetTotalValue = appDetails.allAssets.flatMap(_.other.flatMap(_.value)).getOrElse(BigDecimal(0))
    for (a <- createAsset(AssetDetails.AssetCodeOtherAsset, otherAssetTotalValue)) yield {
      assets += a
    }
  }


  /**
   * Create joint assets for the supplied ApplicationDetails and Asset's listBuffer
   * @param appDetails
   * @param assets
   * @return
   */
  private def buildJointAssets(appDetails: ApplicationDetails, assets: ListBuffer[Asset])={

    // Create asset for jointly owned money
    val jointMoneyValue = appDetails.allAssets.flatMap( _.money.flatMap(_.shareValue)).getOrElse(BigDecimal(0))
    for (jointMoneyAsset <- createAsset(AssetDetails.AssetCodeMoney, jointMoneyValue,
      getOrException(Constants.IHTReturnHowHeld.get(Constants.howHeldJoint)))) yield {
      assets += jointMoneyAsset
    }

    // Create asset for jointly owned household items
    val jointHouseHoldValue = appDetails.allAssets.flatMap( _.household.flatMap(_.shareValue)).getOrElse(BigDecimal(0))
    val jointMotorVehiclesValue = appDetails.allAssets.flatMap( _.vehicles.flatMap(_.shareValue)).getOrElse(BigDecimal(0))
    val totalJointHouseHoldItemsValue = jointHouseHoldValue + jointMotorVehiclesValue

    for (jointHouseHoldAsset <- createAsset(AssetDetails.AssetCodeHouseHold, totalJointHouseHoldItemsValue,
      getOrException(Constants.IHTReturnHowHeld.get(Constants.howHeldJoint)))) yield {
      assets += jointHouseHoldAsset
    }

    // Create asset for jointly owned insurance policies
    val jointInsurancePoliciesValue = appDetails.allAssets.flatMap( _.insurancePolicy.flatMap(_.shareValue)).getOrElse(BigDecimal(0))
    for (jointInsurancePolicy <- createAsset(AssetDetails.AssetCodeInsurancePolicy, jointInsurancePoliciesValue,
      getOrException(Constants.IHTReturnHowHeld.get(Constants.howHeldJoint)))) yield {
      assets += jointInsurancePolicy
    }
  }

  private def createAsset(assetCode: String, totalValue: BigDecimal, howHeld: String = Constants.howHeldStandard):
  Option[Asset] = {
    if (totalValue > 0) {
      Some(Asset(
        assetCode = Some(assetCode),
        assetDescription = AssetDetails.IHTReturnAssetDescription.get(assetCode),
        assetID = Some("null"),
        assetTotalValue = Some(totalValue),
        howheld = Some(howHeld))
      )
    } else {
      None
    }
  }

  /**
   * Creates AddressOrOtherLandLocation from Property
   * @param prop
   * @return
   */
  private def buildAddressOrOtherLandLocation(prop:Property):Option[AddressOrOtherLandLocation]={

    val address= getOrException(prop.address)
    Some(AddressOrOtherLandLocation(addressLine1=Some(address.ukAddressLine1),
      addressLine2 = Some(address.ukAddressLine2),
      addressLine3 = address.ukAddressLine3,
      addressLine4 = address.ukAddressLine4,
      postalCode= Some(address.postCode),
      countryCode=Some(address.countryCode)))
  }

  /**
   * Create Liabilities model
   * @param ad
   * @return
   */
  private def buildLiabilities(ad:ApplicationDetails):Seq[Liability] = {
    val liabilities = new scala.collection.mutable.ListBuffer[Liability]()
    var liabilityValue = BigDecimal(0)
    var liabilityOtherTotal = BigDecimal(0)

    liabilityValue = ad.allLiabilities.flatMap(_.funeralExpenses).map(_.value.getOrElse(BigDecimal(0))).getOrElse(BigDecimal(0))
    if (liabilityValue > 0) {
      liabilities += Liability(
        liabilityType=Some("Funeral Expenses"),
        liabilityAmount=Some(liabilityValue),
        liabilityOwner=Some(Constants.IHTReturnDummyLiabilityOwner))
    }

    liabilityValue = ad.allLiabilities.flatMap(_.trust).map(_.value.getOrElse(BigDecimal(0))).getOrElse(BigDecimal(0))
    if (liabilityValue > 0) {
      liabilityOtherTotal += liabilityValue
    }

    liabilityValue = ad.allLiabilities.flatMap(_.debtsOutsideUk).map(_.value.getOrElse(BigDecimal(0))).getOrElse(BigDecimal(0))
    if (liabilityValue > 0) {
      liabilityOtherTotal += liabilityValue
    }

    liabilityValue = ad.allLiabilities.flatMap(_.jointlyOwned).map(_.value.getOrElse(BigDecimal(0))).getOrElse(BigDecimal(0))
    if (liabilityValue > 0) {
      liabilityOtherTotal += liabilityValue
    }

    liabilityValue = ad.allLiabilities.flatMap(_.other).map(_.value.getOrElse(BigDecimal(0))).getOrElse(BigDecimal(0))
    if (liabilityValue > 0) {
      liabilityOtherTotal += liabilityValue
    }

    if (liabilityOtherTotal > 0) {
      liabilities += Liability(liabilityType = Some("Other"),
        liabilityAmount = Some(liabilityOtherTotal), liabilityOwner = Some(Constants.IHTReturnDummyLiabilityOwner))
    }

    liabilities.toSeq
  }

  /**
   * Create exemption model
   * @param ad
   * @return
   */
  private def buildExemptions(ad:ApplicationDetails):Seq[Exemption] = {
    val exemptions = new scala.collection.mutable.ListBuffer[Exemption]()
    var totalAssets:BigDecimal = BigDecimal(0)

    totalAssets = ad.allExemptions.flatMap(_.partner).map(_.totalAssets.getOrElse(BigDecimal(0))).getOrElse(BigDecimal(0))
    if (totalAssets>0) {
      exemptions += Exemption(
        exemptionType=Some("Spouse"),
        percentageAmount=None,
        overrideValue=Some(totalAssets)
      )
    }

    for (c<-ad.charities) yield {
      val totalValue = c.totalValue
      exemptions += Exemption(
        exemptionType=Some("Charity"),
        percentageAmount=None,
        overrideValue=Some(totalValue.getOrElse(BigDecimal(0)))
      )
    }

    for (c<-ad.qualifyingBodies) yield {
      val totalValue = c.totalValue
      exemptions += Exemption(
        exemptionType=Some("GNCP"),
        percentageAmount=None,
        overrideValue=totalValue
      )
    }
    exemptions.toSeq
  }

  /**
   * Returns None if Partner's home is not in UK
   * @param ad - ApplicationDetails
   * @return - Option[PartnerExemption]
   */
  private def getPartnerExemption(ad: ApplicationDetails): Option[PartnerExemption] = {

    ad.allExemptions.flatMap(_.partner) match {
      case Some(pe) => {
        if(pe.isAssetForDeceasedPartner.getOrElse(false)) Some(pe) else None
      }
      case _ => None
    }
  }

  /**
   * Create Gifts
   * @param ad
   * @param dateOfDeath
   * @return
   */
  def buildGifts(ad:ApplicationDetails, dateOfDeath: LocalDate):Option[Set[Set[Gift]]] = {
    // Add all the gifts from section 3 of Gifts front-end.
    val giftsSection3 = if (ad.giftsList.isDefined && ad.allGifts.isDefined) {
      val gifts:Seq[Gift] = for (gift <- getOrException(ad.giftsList) if gift.value.getOrElse(BigDecimal(0)) > 0 ) yield {
        val totalGiftValue = gift.value.fold(BigDecimal(0))(x => x) - gift.exemptions.fold(BigDecimal(0))(x => x)
        val assetDescription = if(gift.exemptions.fold(BigDecimal(0))(x => x) > 0) {
          Some(s"Rolled up gifts minus exemption of £${gift.exemptions.getOrElse(BigDecimal(0))}")
        } else {
          Some(s"Rolled up gifts")
        }

        Gift(
          assetCode=Some(AssetDetails.AssetCodeGift),
          assetDescription=assetDescription,
          assetID=Some("null"),
          assetTotalValue = Some(totalGiftValue),
          valuePrevOwned = gift.value,
          percentageSharePrevOwned = Some(BigDecimal(100)),
          valueRetained = Some(BigDecimal(0)),
          percentageRetained = Some(BigDecimal(0)),
          howheld = Some("Standard"),
          lossToEstate = Some(totalGiftValue),
          dateOfGift = Some(dateLongFormatToDesString(gift.endDate.getOrElse("")))
        )
      }
      gifts
    } else {
      Nil
    }

    giftsSection3 match {
      case Nil => None
      case _ => Some(Set(giftsSection3.toSet))
    }
  }

  /**
   * Create the trust fir given applicationdetails
   * @param ad - ApplicationDetails
   * @return
   */
  def buildTrusts(ad:ApplicationDetails):Option[Set[Trust]] = {
    // Add trusts from assets held in trust

    val response = for {
      allAssets: AllAssets <- ad.allAssets
      heldInTrust: HeldInTrust <- allAssets.heldInTrust
      value: BigDecimal <- heldInTrust.value
      asset: Asset <- createAsset(AssetDetails.AssetCodeTrust, value)
    }yield{
        Set(Trust(trustName=Some("Deceased Trust"), trustAssets= Some(Set(asset))))
      }
    response.orElse(None)
  }


  /**
   * Create the Declaration model
   * @param ad
   * @param declarationDate
   * @return
   */
  def buildDeclaration(ad:ApplicationDetails, declarationDate:LocalDateTime) = {
    Declaration(
      reasonForBeingBelowLimit= ad.reasonForBeingBelowLimit,
      declarationAccepted= Some(true),
      coExecutorsAccepted= Some(true),
      declarationDate= Some(dateTimeToDesString(declarationDate)))
  }
}
