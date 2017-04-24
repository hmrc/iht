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

package constants
import scala.collection.immutable.ListMap

/**
 * Created by xavierzanatta on 3/25/15.
 */
object Constants {
  val monthNameToNumber = Map(
    "January"   -> 1,
    "February"  -> 2,
    "March"     -> 3,
    "April"     -> 4,
    "May"       -> 5,
    "June"      -> 6,
    "July"      -> 7,
    "August"    -> 8,
    "September" -> 9,
    "October"   -> 10,
    "November"  -> 11,
    "December"  -> 12
  )

  val MaritalStatusMarried = "Married or in Civil Partnership"
  val MaritalStatusSingle = "Single"
  val MaritalStatusDivorced = "Divorced or Former Civil Partner"
  val MaritalStatusWidowed = "Widowed or a Surviving Civil Partner"

  val PropertyTypeDeceasedHome = "Deceased's home"
  val PropertyTypeOtherResidentialBuilding = "Other residential building"
  val PropertyTypeNonResidential = "Land, non-residential or business building"

  val IHTReturnDateFormat = "YYYY-MM-dd"

  // Default values for DES schema generation for real-time risking.
  val RiskInputEventType = "death"
  val RiskInputEntryType = "Free Estate"

  // Dummy values for mandatory fields I can't find source for:-
  val IHTReturnDummyDateOfMarriage = "1670-12-01" // Surviving spouse
  val IHTReturnDummyDateOfBirth = "1670-12-01" // Deceased spouse
  val IHTReturnDummyDateOfDeath = "1670-12-01" // Surviving spouse
  val IHTReturnDummyLiabilityOwner = ""
  val IHTReturnDummyDomicile = "England or Wales"

  val IHTReturnSubmitterRole = "Lead Executor"

  val IHTReturnPropertyAssetDescription = ListMap(
    PropertyTypeDeceasedHome -> "Deceased's residence",
    PropertyTypeOtherResidentialBuilding -> "Other residential property",
    PropertyTypeNonResidential -> "Other land and buildings")

  val IHTReturnPropertyAssetCode = ListMap(
    PropertyTypeDeceasedHome -> "0016",
    PropertyTypeOtherResidentialBuilding -> "0017",
    PropertyTypeNonResidential -> "0018")

  // Howheld values

  val howHeldStandard = "Standard"
  val howHeldNominated = "Nominated"
  val howHeldForeign = "Foreign"
  val howHeldJoint = "Joint"

  val IHTReturnHowHeld = ListMap(
    "Deceased only" -> "Standard",
    "Joint" -> "Joint - Beneficial Joint Tenants",
    "In common" -> "Joint - Tenants In Common")

  val ReasonForBeingBelowLimitExceptedEstate = "Excepted Estate"
  val ReasonForBeingBelowLimitTNRB = "Transferred Nil Rate Band"
  val ReasonForBeingBelowLimitSpouseCivilPartnerOrCharityExemption  = "Spouse, Civil Partner or Charity Exemption"


  val NilRateBand = BigDecimal(325000)
  val GrossEstateLimit = BigDecimal(1000000)
  val TransferredNilRateBand = BigDecimal(650000)

  val ServerError="CAN NOT PARSE IHT REF FROM RESPONSE "
  val ServerErrorFailure="CAN NOT PARSE IHT REF FROM FAILURE RESPONSE "
  val JasonValidationError="Not submitted: JSON validation against schema failed:-"

  //fieldMapping keys
  val ApplicantCountryEnglandOrWales = "England or Wales"
  val ApplicantCountryScotland = "Scotland"
  val ApplicantCountryNorthernIreland = "Northern Ireland"
  val RoleExecutor = "Executor"
  val RoleLeadExecutor = "Lead Executor"

  //IHT Home
  val AppStatusAwaitingReturn="Awaiting Return"
  val AppStatusNotStarted="Not Started"
  val AppStatusInProgress="In Progress"
  val AppStatusInReview="In Review"
  val AppStatusClosed="Closed"
  val AppStatusKickOut="Kick Out"

  // Audit keys

  val RegSubmissionRequestKey="RegistrationSubmissionRequest"
  val RegSubmissionFailureResponseKey= "RegistrationSubmissionFailureResponse"
  val AppSubmissionRequestKey= "ApplicationSubmissionRequest"
  val AppSubmissionFailureResponseKey= "ApplicationSubmissionFailureResponse"

  // Schema paths
  val schemaPathRegistrationSubmission = "/schemas/Registration_RequestSchema_v0.7.json"
  val schemaPathApplicationSubmission = "/schemas/Submit_IHT_Return_RequestSchema_v0.9.json"
  val schemaPathRealTimeRisking = "/schemas/Risk Input Schema_v0.14.json"
  val schemaPathClearanceRequest = "/schemas/Request_IHT_Case_Clearance_RequestSchema_v0.2.json"
  val schemaPathClearanceResponse = "/schemas/Request_IHT_Case_Clearance_ResponseSchema_v0.2.json"
  val schemaPathIhtReturn = "/schemas/Get_IHT_ReturnDetails_ResponseSchema_v0.9.json"
  val schemaPathProbateDetails = "/schemas/Notify_Courts_Service_Schema_v0.2.json"
  val schemaPathCaseDetails = "/schemas/Get_IHT_CaseDetails_ResponseSchema_v0.8.json"
  val schemaPathListCases = "/schemas/List_IHT_Cases_ResponseSchema_v0.5.json"

  // To be removed once EDH schema is fixed at EDH side.
  val modifiedMaritalStatusForEdh =  ListMap (
    MaritalStatusMarried -> "Married or in Civil Partnership",
    MaritalStatusSingle -> "Single",
    MaritalStatusDivorced -> "Divorced or former Civil Partner",
    MaritalStatusWidowed -> "Widowed or a surviving civil partner"
  )

  // audit even value keys
  val AuditTypeCurrencyValueChange = "currencyValueChange"

  val AuditTypePreviousValue = "previousValue"
  val AuditTypeNewValue = "newValue"
  val AuditTypeValue = "value"

  val AuditTypeProperties = "properties"
  val AuditTypeMoney = "money"
  val AuditTypeMoneyShared = "moneyShared"
  val AuditTypeHousehold = "household"
  val AuditTypeHouseholdShared = "householdShared"
  val AuditTypeMotorVehicles = "motorVehicles"
  val AuditTypeMotorVehiclesShared = "motorVehiclesShared"
  val AuditTypePrivatePensions = "privatePensions"
  val AuditTypeStocksAndSharesListed = "stocksAndSharesListed"
  val AuditTypeStocksAndSharesNotListed = "stocksAndSharesNotListed"
  val AuditTypeInsurancePolicies = "insurancePolicies"
  val AuditTypeInsurancePoliciesJointlyHeld = "insurancePoliciesJointlyHeld"
  val AuditTypeBusinessInterests = "businessInterests"
  val AuditTypeNominatedAssets = "nominatedAssets"
  val AuditTypeAssetsHeldInTrust = "assetsHeldInTrust"
  val AuditTypeForeignAssets = "foreignAssets"
  val AuditTypeMoneyOwed = "moneyOwed"
  val AuditTypeOtherAssets = "otherAssets"

  val AuditTypeMortgages = "mortgages"
  val AuditTypeFuneralExpenses = "funeralExpenses"
  val AuditTypeDebtsOwedFromATrust = "debtsOwedFromATrust"
  val AuditTypeDebtsOwedToAnyoneOutsideUK = "debtsOwedToAnyoneOutsideUK"
  val AuditTypeDebtsOwedOnJointlyOwnedAssets = "debtsOwedOnJointlyOwnedAssets"
  val AuditTypeOtherDebts = "otherDebts"

  val AuditTypeExemptionPartner = "exemptionPartner"
  val AuditTypeExemptionCharities = "exemptionCharities"
  val AuditTypeExemptionQualfifyingBodies = "exemptionQualifyingBodies"

  val AuditTypeGifts = "gifts"

  val AuditTypeFinalEstateValue = "finalEstateValue"
}

object AssetDetails {
  val AssetCodeMoney = "9001"
  val AssetCodeHouseHold = "9004"
  val AssetCodePrivatePension = "9005"
  val AssetCodeStockShareNotListed = "9010"
  val AssetCodeStockShareListed = "9008"
  val AssetCodeInsurancePolicy = "9006"
  val AssetCodeBusinessInterest = "9021"
  val AssetCodeNominatedAsset = "9099"
  val AssetCodeForeignAsset = "9098"
  val AssetCodeMoneyOwed = "9013"
  val AssetCodeOtherAsset = "9015"
  val AssetCodeTrust = "9097"
  val AssetCodeGift = "9095"

  val IHTReturnAssetCodeBankAndBuildingSocietyAccounts = "9001"
  val IHTReturnRuleIDBankAndBuildingSocietyAccounts = "2"

  val IHTReturnAssetDescription = ListMap(
    IHTReturnAssetCodeBankAndBuildingSocietyAccounts -> "Rolled up bank and building society accounts",
    AssetCodeHouseHold -> "Rolled up household and personal goods",
    AssetCodePrivatePension -> "Rolled up pensions",
    AssetCodeStockShareNotListed -> "Rolled up unlisted stocks and shares",
    AssetCodeStockShareListed -> "Rolled up quoted stocks and shares",
    AssetCodeInsurancePolicy -> "Rolled up life assurance policies",
    AssetCodeBusinessInterest -> "Rolled up business assets",
    AssetCodeNominatedAsset -> "Rolled up nominated assets",
    AssetCodeTrust -> "Rolled up trust assets",
    AssetCodeForeignAsset -> "Rolled up foreign assets",
    AssetCodeMoneyOwed -> "Rolled up money owed to deceased",
    AssetCodeOtherAsset -> "Rolled up other assets"
  )
}
