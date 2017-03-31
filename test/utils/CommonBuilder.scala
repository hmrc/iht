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

import models._

import models.application._
import org.joda.time.{LocalDateTime, DateTime, LocalDate}
import constants.Constants

/**
 *
 * Created by Vineet Tyagi on 26/05/15.
 *
 */
object CommonBuilder {
  val DefaultId="1"
  val DefaultDeceasedDOD=new LocalDate(1987,12,12)
  val DefaultFirstName="XYZAB"
  val DefaultMiddleName=""
  val DefaultLastName="ABCXY"
  val DefaultNino=NinoBuilder.defaultNino
  val DefaultUtr=None
  val DefaultDateOfBirth=new LocalDate(1998,12,12)
  val DefaultUkAddress=new UkAddress("addr1", "addr2", None, None, "AA1 1AA", "GB")
  val DefaultPhoneNo = "07000111222"
  val DefaultContactDetails=new ContactDetails("07000111222", "a@example.com")
  val DefaultCountry="England or Wales"
  val DefaultRole="Executor"
  val DefaultCoExecutorRole="SECONDARY_EXECUTOR"
  val DefaultDomicile="0001"
  val DefaultMaritalStatus=Constants.MaritalStatusMarried

  //Default values for Application Model
  val DefaultIsAssetForDeceasedPartner=Some(true)
  val DefaultIsPartnerHomeInUK=Some(true)
  val DefaultTotalAssets=BigDecimal(120)

  // Default values for Return details
  val DefaultReturnId = "1234567890"
  val DefaultReturnVersionNumber = "0123"
  val DefaultReturnDate = "2015-05-01"
  val DefaultSubmitterRole = "Lead Executor"

  // Creates the DeceasedDateOfDeath with default values
  val buildDeceasedDateOfDeath=new DeceasedDateOfDeath(
    dateOfDeath=DefaultDeceasedDOD)

  // Creates the ApplicantDetails with default values
  val buildApplicantDetails = ApplicantDetails (
    firstName=DefaultFirstName,
    middleName=Some(DefaultMiddleName),
    lastName=DefaultLastName,
    nino=DefaultNino,
    dateOfBirth=DefaultDateOfBirth,
    ukAddress=DefaultUkAddress,
    phoneNo=Some(DefaultPhoneNo),
    country= DefaultCountry,
    role= DefaultRole
  )

  // Creates the DeceasedDetails with default values
  val buildDeceasedDetails = DeceasedDetails(
    firstName=DefaultFirstName,
    middleName=Some(DefaultMiddleName),
    lastName=DefaultLastName,
    nino=DefaultNino,
    ukAddress=DefaultUkAddress,
    dateOfBirth=DefaultDateOfBirth,
    domicile=DefaultDomicile,
    maritalStatus=DefaultMaritalStatus)

  // Creates the CoExecutor with default values
  val buildCoExecutor = CoExecutor(
    id=Some(DefaultId),
    firstName=DefaultFirstName,
    middleName=None,
    lastName=DefaultLastName,
    dateOfBirth=DefaultDateOfBirth,
    nino=DefaultNino,
    ukAddress=DefaultUkAddress,
    contactDetails=DefaultContactDetails,
    role =Some(DefaultCoExecutorRole))

  // Creates Return Details with default values
  val buildReturnDetails = ReturnDetails(
    returnDate = Some(DefaultReturnDate),
    returnId = Some(DefaultReturnId),
    returnVersionNumber = Some(DefaultReturnVersionNumber),
    submitterRole = DefaultSubmitterRole
  )
  // Creates the RegistrationDetails with default values
  val buildRegistrationDetails = RegistrationDetails(
    deceasedDateOfDeath=None,
    applicantDetails=None,
    deceasedDetails=None,
    coExecutors = Seq(),
    ihtReference = None,
    returns = Seq(buildReturnDetails)
  )

  val buildRegistrationDetailsDODandDeceasedDetails = {
    RegistrationDetails(
      deceasedDateOfDeath=Some(CommonBuilder.buildDeceasedDateOfDeath),
      applicantDetails=None,
      deceasedDetails=Some(CommonBuilder.buildDeceasedDetails copy(nino="") ),
      coExecutors = Nil,
      ihtReference = Some("ABC"),
      returns = Seq(buildReturnDetails)
    )
  }

  //Creates the ApplicationDetails with default values
  val buildApplicationDetailsEmpty=ApplicationDetails(
    allAssets=None,
    propertyList=Nil,
    allLiabilities=None,
    allExemptions=None,
    charities= Seq(),
    qualifyingBodies = Seq(),
    widowCheck=None,
    increaseIhtThreshold=None,
    status= constants.Constants.AppStatusNotStarted,
    kickoutReason=None
  )

  val buildAllAssets = AllAssets(
    action=None,
    money=Some(ShareableBasicEstateElement(value=Some(BigDecimal(1)), shareValue=Some(BigDecimal(2)))),
    household=Some(ShareableBasicEstateElement(value=Some(BigDecimal(3)), shareValue=Some(BigDecimal(4)))),
     vehicles=Some(ShareableBasicEstateElement(value=Some(BigDecimal(5)), shareValue=Some(BigDecimal(6)))),
    stockAndShare=Some(StockAndShare(valueNotListed=Some(BigDecimal(9)),
      valueListed=Some(BigDecimal(10)), value=Some(BigDecimal(11)))),
    privatePension=Some(PrivatePension(isChanged=Some(false), value=Some(BigDecimal(7)))),
    insurancePolicy=Some(InsurancePolicy(isAnnuitiesBought=Some(false),
      isInsurancePremiumsPayedForSomeoneElse=Some(false),
      value=Some(BigDecimal(12)), shareValue=Some(BigDecimal(13)), None, None, None, None, None, None )),
    businessInterest=Some(BasicEstateElement(value=Some(BigDecimal(14)))),
    nominated=Some(BasicEstateElement(value=Some(BigDecimal(16)))),
    heldInTrust=Some(HeldInTrust(isMoreThanOne=Some(false),value=Some(BigDecimal(17)))),
    foreign=Some(BasicEstateElement(value=Some(BigDecimal(18)))),
    moneyOwed=Some(BasicEstateElement(value=Some(BigDecimal(15)))),
    other=Some(BasicEstateElement(value=Some(BigDecimal(19))))
  )

  val buildPropertyList=List(
    Property(id=Some("1"), address=Some(DefaultUkAddress), propertyType=Some("Deceased's home"),
      typeOfOwnership=Some("Deceased only"), tenure=Some("Freehold"), value=Some(BigDecimal(100))),
    Property(id=Some("2"), address=Some(DefaultUkAddress), propertyType=Some("Other residential building"),
      typeOfOwnership=Some("Joint"), tenure=Some("Leasehold"), value=Some(BigDecimal(200))),
    Property(id=Some("3"), address=Some(DefaultUkAddress), propertyType=Some("Land, non-residential or business building"),
      typeOfOwnership=Some("In common"), tenure=Some("Leasehold"), value=Some(BigDecimal(300)))
  )

  val buildPropertyListInvalidPropertyType=List(
    Property(id=Some("1"), address=Some(DefaultUkAddress), propertyType=None,
      typeOfOwnership=Some("Only by the deceased"), tenure=Some("Freehold"), value=Some(BigDecimal(100)))
  )

  val buildMortgageList = List(
      Mortgage(id="1", value=Some(BigDecimal(80))),
      Mortgage(id="2", value=Some(BigDecimal(150)))
    )

  val buildAllLiabilities = AllLiabilities(
    funeralExpenses= Some(BasicEstateElementLiabilities(isOwned=Some(true), Some(BigDecimal(20)))),
    trust= Some(BasicEstateElementLiabilities(isOwned=Some(true), Some(BigDecimal(21)))),
    debtsOutsideUk= Some(BasicEstateElementLiabilities(isOwned=Some(true), Some(BigDecimal(22)))),
    jointlyOwned= Some(BasicEstateElementLiabilities(isOwned=Some(true), Some(BigDecimal(23)))),
    other= Some(BasicEstateElementLiabilities(isOwned=Some(true), Some(BigDecimal(24)))),
    mortgages= Some(MortgageEstateElement(isOwned=Some(true), mortgageList=buildMortgageList)))

  val buildAllExemptions = AllExemptions(
    partner= Some(PartnerExemption(
      isAssetForDeceasedPartner= Some(true),
      isPartnerHomeInUK= Some(true),
      firstName= Some("ABCDE"),
      lastName= Some("XYZAB"),
      dateOfBirth= Some(new LocalDate(2011,11,12)),
      nino= Some(DefaultNino),
      totalAssets= Some(BigDecimal(25)))),
    charity= Some(BasicExemptionElement(isSelected=Some(true))),
    qualifyingBody= Some(BasicExemptionElement(isSelected=Some(true)))  )

  val buildGiftsList = Seq(
    PreviousYearsGifts(
      yearId=Some("1"),
      value= Some(BigDecimal(1000)),
      exemptions = Some(BigDecimal(0)),
      startDate= Some("6 April 2004"),
      endDate= Some("5 April 2005")
    ),
    PreviousYearsGifts(
      yearId=Some("2"),
      value= Some(BigDecimal(2000)),
      exemptions = Some(BigDecimal(200)),
      startDate= Some("6 April 2005"),
      endDate= Some("5 April 2006")
    ),
    PreviousYearsGifts(
      yearId=Some("3"),
      value= Some(BigDecimal(3000)),
      exemptions = Some(BigDecimal(0)),
      startDate= Some("6 April 2006"),
      endDate= Some("5 April 2007")
    ),
    PreviousYearsGifts(
      yearId=Some("4"),
      value= Some(BigDecimal(4000)),
      exemptions = Some(BigDecimal(0)),
      startDate= Some("6 April 2007"),
      endDate= Some("5 April 2008")
    ),
    PreviousYearsGifts(
      yearId=Some("5"),
      value= Some(BigDecimal(5000)),
      exemptions = Some(BigDecimal(0)),
      startDate= Some("6 April 2008"),
      endDate= Some("5 April 2009")
    ),
    PreviousYearsGifts(
      yearId=Some("6"),
      value= Some(BigDecimal(6000)),
      exemptions = Some(BigDecimal(0)),
      startDate= Some("6 April 2009"),
      endDate= Some("5 April 2010")
    ),
    PreviousYearsGifts(
      yearId=Some("7"),
      value= Some(BigDecimal(7000)),
      exemptions = Some(BigDecimal(0)),
      startDate= Some("6 April 2010"),
      endDate= Some("5 April 2011")
    )
  )

  val buildCharitiesSeq1Item = Seq(
    Charity(id= Some("1"), name= Some("Charity 1"), number= Some("111"), totalValue= Some(BigDecimal(26))  ))

  val buildCharitiesSeq2Items = Seq(
    Charity(id= Some("1"), name= Some("Charity 2"), number= Some("222"), totalValue= Some(BigDecimal(27) ) ),
    Charity(id= Some("2"), name= Some("Charity 3"), number= Some("333"), totalValue= Some(BigDecimal(28) ) )
  )

  val buildQualifyingBodySeq1Item = Seq(
    QualifyingBody( id=Some("1"), name=Some("Qualifying body 1"), totalValue=Some(BigDecimal(29)))
  )

  val buildQualifyingBodySeq2Items = Seq(
    QualifyingBody( id=Some("1"), name=Some("Qualifying body 2"), totalValue=Some(BigDecimal(30))),
    QualifyingBody( id=Some("2"), name=Some("Qualifying body 3"), totalValue=Some(BigDecimal(31)))
  )

  val buildWidowCheck = WidowCheck(widowed=Some(true), dateOfPreDeceased=Some(new LocalDate(2010,10,12)))

  val buildIncreaseIhtThreshold = TnrbEligibiltyModel(
    isPartnerLivingInUk= Some(true),
    isGiftMadeBeforeDeath= Some(false),
    isStateClaimAnyBusiness= Some(true),
    isPartnerGiftWithResToOther= Some(false),
    isPartnerBenFromTrust= Some(true),
    isEstateBelowIhtThresholdApplied= Some(false),
    isJointAssetPassed= Some(true),
    firstName= Some("ABCXYZ"),
    lastName= Some("XYZABC"),
    dateOfMarriage= Some(new LocalDate(2008,12,13)),
    dateOfPreDeceased= Some(new LocalDate(2006,12,13)))
  
  val buildApplicationDetailsAllFields=ApplicationDetails(
    allAssets=Some(buildAllAssets),
    propertyList=buildPropertyList,
    allLiabilities=Some(buildAllLiabilities),
    allExemptions=Some(buildAllExemptions),
    allGifts=Some(AllGifts(
      isReservation=Some(true),
      isToTrust=Some(false),
      isGivenInLast7Years=Some(true),
      action= Some("")
    )),
    giftsList=Some(buildGiftsList),
    charities= buildCharitiesSeq2Items,
    qualifyingBodies = buildQualifyingBodySeq2Items,
    widowCheck=Some(buildWidowCheck),
    increaseIhtThreshold=Some(buildIncreaseIhtThreshold),
    status= constants.Constants.AppStatusInProgress,
    kickoutReason=None,
    ihtRef=None,
    reasonForBeingBelowLimit = Some(constants.Constants.ReasonForBeingBelowLimitExceptedEstate)
  )

  val buildAllAssetsTotal1322860 = AllAssets(
    action=None,
    money=Some(ShareableBasicEstateElement(value=Some(BigDecimal(226155)), shareValue=Some(BigDecimal(200)))),
    household=Some(ShareableBasicEstateElement(value=Some(BigDecimal(300)), shareValue=Some(BigDecimal(400)))),
    vehicles=Some(ShareableBasicEstateElement(value=Some(BigDecimal(1544)), shareValue=Some(BigDecimal(63)))),
    privatePension=Some(PrivatePension(isChanged=Some(false),
      value=Some(BigDecimal(70000)))),
    stockAndShare=None,
    insurancePolicy=Some(InsurancePolicy(isAnnuitiesBought=Some(false),
      isInsurancePremiumsPayedForSomeoneElse=Some(false),
      value=Some(BigDecimal(2000)), shareValue=Some(BigDecimal(1300)), None, None, None, None, None, None)),
    businessInterest=Some(BasicEstateElement(value=Some(BigDecimal(999900)))),
    moneyOwed=None,
    nominated=None,
    heldInTrust=None,
    foreign=Some(BasicEstateElement(value=Some(BigDecimal(18000)))),
    other=Some(BasicEstateElement(value=Some(BigDecimal(1998))))
  ) // Total = £1,322,860

  val buildAllAssetsTotal329100 = AllAssets(
    action=None,
    money=Some(ShareableBasicEstateElement(value=Some(BigDecimal(226155)), shareValue=Some(BigDecimal(200)))),
    household=Some(ShareableBasicEstateElement(value=Some(BigDecimal(3300)), shareValue=Some(BigDecimal(400)))),
    vehicles=Some(ShareableBasicEstateElement(value=Some(BigDecimal(1544)), shareValue=Some(BigDecimal(63)))),
    privatePension=Some(PrivatePension(isChanged=Some(false),
      value=Some(BigDecimal(70000)))),
    stockAndShare=None,
    insurancePolicy=Some(InsurancePolicy(isAnnuitiesBought=Some(false),
      isInsurancePremiumsPayedForSomeoneElse=Some(false),
      value=Some(BigDecimal(2000)), shareValue=Some(BigDecimal(1300)),  None, None, None, None, None, None )),
    businessInterest=Some(BasicEstateElement(value=Some(BigDecimal(3140)))),
    moneyOwed=None,
    nominated=None,
    heldInTrust=None,
    foreign=Some(BasicEstateElement(value=Some(BigDecimal(18000)))),
    other=Some(BasicEstateElement(value=Some(BigDecimal(1998))))
  ) // Total = £326100

  val buildAllAssetsTotal122960 = AllAssets(
    action=None,
    money=Some(ShareableBasicEstateElement(value=Some(BigDecimal(26155)), shareValue=Some(BigDecimal(200)))),
    household=Some(ShareableBasicEstateElement(value=Some(BigDecimal(300)), shareValue=Some(BigDecimal(400)))),
    vehicles=Some(ShareableBasicEstateElement(value=Some(BigDecimal(1544)), shareValue=Some(BigDecimal(63)))),
    privatePension=Some(PrivatePension(isChanged=Some(false),
      value=Some(BigDecimal(70000)))),
    stockAndShare=None,
    insurancePolicy=Some(InsurancePolicy(isAnnuitiesBought=Some(false),
      isInsurancePremiumsPayedForSomeoneElse=Some(false),
      value=Some(BigDecimal(2000)), shareValue=Some(BigDecimal(1300)),  None, None, None, None, None, None )),
    businessInterest=None,
    moneyOwed=None,
    nominated=None,
    heldInTrust=None,
    foreign=Some(BasicEstateElement(value=Some(BigDecimal(18000)))),
    other=Some(BasicEstateElement(value=Some(BigDecimal(1998))))
  ) // Total = £122,960

  val buildPropertyListTotal3000=List(
    Property(id=Some("1"), address=Some(DefaultUkAddress), propertyType=Some("Deceased's home"),
      typeOfOwnership=Some("Only by the deceased"), tenure=Some("Freehold"), value=Some(BigDecimal(1000))),
    Property(id=Some("2"), address=Some(DefaultUkAddress), propertyType=Some("Other residential building"),
      typeOfOwnership=Some("Only by the deceased"), tenure=Some("Leasehold"), value=Some(BigDecimal(2000)))
  ) // Total = £3,000

  val buildPropertyListTotal325000=List(
    Property(id=Some("1"), address=Some(DefaultUkAddress), propertyType=Some("Deceased's home"),
      typeOfOwnership=Some("Only by the deceased"), tenure=Some("Freehold"), value=Some(BigDecimal(300000))),
    Property(id=Some("2"), address=Some(DefaultUkAddress), propertyType=Some("Other residential building"),
      typeOfOwnership=Some("Only by the deceased"), tenure=Some("Leasehold"), value=Some(BigDecimal(25000)))
  ) // Total = £3,000

  val buildPropertyListTotal6000=List(
    Property(id=Some("1"), address=Some(DefaultUkAddress), propertyType=Some("Deceased's home"),
      typeOfOwnership=Some("Only by the deceased"), tenure=Some("Freehold"), value=Some(BigDecimal(2000))),
    Property(id=Some("2"), address=Some(DefaultUkAddress), propertyType=Some("Other residential building"),
      typeOfOwnership=Some("Only by the deceased"), tenure=Some("Leasehold"), value=Some(BigDecimal(4000)))
  ) // Total = £3,000

  val buildPropertyListTotal30000=List(
    Property(id=Some("1"), address=Some(DefaultUkAddress), propertyType=Some("Deceased's home"),
      typeOfOwnership=Some("Only by the deceased"), tenure=Some("Freehold"), value=Some(BigDecimal(10000))),
    Property(id=Some("2"), address=Some(DefaultUkAddress), propertyType=Some("Other residential building"),
      typeOfOwnership=Some("Only by the deceased"), tenure=Some("Leasehold"), value=Some(BigDecimal(20000)))
  ) // Total = £30,000

  val buildAllExemptionsTotal3000 = AllExemptions(
    partner= Some(PartnerExemption(
      isAssetForDeceasedPartner= Some(true),
      isPartnerHomeInUK= Some(true),
      firstName= Some("ABCDE"),
      lastName= Some("XYZAC"),
      dateOfBirth= Some(new LocalDate(2011,11,12)),
      nino= Some(CommonBuilder.DefaultNino),
      totalAssets= Some(BigDecimal(3000)))),
    charity= Some(BasicExemptionElement(isSelected=Some(true))),
    qualifyingBody= Some(BasicExemptionElement(isSelected=Some(true)))  )

  val buildAllExemptionsTotal300000 = AllExemptions(
    partner= Some(PartnerExemption(
      isAssetForDeceasedPartner= Some(true),
      isPartnerHomeInUK= Some(true),
      firstName= Some("ABCDE"),
      lastName= Some("XYZAB"),
      dateOfBirth= Some(new LocalDate(2011,11,12)),
      nino= Some(CommonBuilder.DefaultNino),
      totalAssets= Some(BigDecimal(300000)))),
    charity= None,
    qualifyingBody= None  )

  val buildCharitiesTotal1000 = Seq(
    Charity(id= Some("1"), name= Some("Charity 2"), number= Some("222"), totalValue= Some(BigDecimal(400))  ),
    Charity(id= Some("2"), name= Some("Charity 3"), number= Some("333"), totalValue= Some(BigDecimal(600))  )
  )

  val buildCharitiesTotal10000 = Seq(
    Charity(id= Some("1"), name= Some("Charity 2"), number= Some("222"), totalValue= Some(BigDecimal(4000))  ),
    Charity(id= Some("2"), name= Some("Charity 3"), number= Some("333"), totalValue= Some(BigDecimal(6000))  )
  )

  val buildQualifyingBodiesTotal1000 = Seq(
    QualifyingBody( id=Some("1"), name=Some("Qualifying body 2"), totalValue=Some(BigDecimal(300))),
    QualifyingBody( id=Some("2"), name=Some("Qualifying body 3"), totalValue=Some(BigDecimal(700)))
  )

  val buildQualifyingBodiesTotal10000 = Seq(
    QualifyingBody( id=Some("1"), name=Some("Qualifying body 2"), totalValue=Some(BigDecimal(3000))),
    QualifyingBody( id=Some("2"), name=Some("Qualifying body 3"), totalValue=Some(BigDecimal(7000)))
  )

  val buildApplicationDetailsReasonForBeingBelowLimitExceptedEstate=ApplicationDetails(
    allAssets=Some(buildAllAssetsTotal122960),
    propertyList=buildPropertyListTotal3000,
    allLiabilities=None,
    allExemptions=None,
    charities= Nil,
    qualifyingBodies = Nil,
    widowCheck=None,
    increaseIhtThreshold=None,
    status= constants.Constants.AppStatusInProgress,
    kickoutReason=None,
    reasonForBeingBelowLimit = Some(constants.Constants.ReasonForBeingBelowLimitExceptedEstate)
  )

  val buildApplicationDetailsReasonForBeingBelowLimitSpouseCivilPartnerCharity=ApplicationDetails(
    allAssets=Some(buildAllAssetsTotal329100),
    propertyList=Nil,
    allLiabilities=None,
    allExemptions=Some(buildAllExemptionsTotal3000),
    charities= buildCharitiesTotal1000,
    qualifyingBodies = buildQualifyingBodiesTotal1000,
    widowCheck=None,
    increaseIhtThreshold=None,
    status= constants.Constants.AppStatusInProgress,
    kickoutReason=None,
    reasonForBeingBelowLimit = Some(constants.Constants.ReasonForBeingBelowLimitSpouseCivilPartnerOrCharityExemption)
  )

  val buildApplicationDetailsReasonForBeingBelowLimitTNRB=ApplicationDetails(
    allAssets=Some(buildAllAssetsTotal329100),
    propertyList=buildPropertyListTotal325000,
    allLiabilities=None,
    allExemptions=Some(buildAllExemptionsTotal300000),
    charities= Nil,
    qualifyingBodies = Nil,
    widowCheck=None,
    increaseIhtThreshold=None,
    status= constants.Constants.AppStatusInProgress,
    kickoutReason=None,
    reasonForBeingBelowLimit = Some(constants.Constants.ReasonForBeingBelowLimitTNRB)
  )

  val buildApplicationDetailsAppStatusKickout=ApplicationDetails(
    allAssets=Some(buildAllAssets),
    propertyList=buildPropertyList,
    allLiabilities=Some(buildAllLiabilities),
    allExemptions=Some(buildAllExemptions),
    charities= buildCharitiesSeq2Items,
    qualifyingBodies = buildQualifyingBodySeq2Items,
    widowCheck=Some(buildWidowCheck),
    increaseIhtThreshold=Some(buildIncreaseIhtThreshold),
    status= constants.Constants.AppStatusKickOut,
    kickoutReason=Some("kicked out")
  )
}
