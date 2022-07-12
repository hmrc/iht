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

package models.des.realtimerisking

import models.registration.RegistrationDetails
import play.api.libs.json.{Json, OFormat}
import constants.{AssetDetails, Constants}

case class RiskInput(acknowledgementReference: Option[String]=None,
                     riskConsidered:Option[Boolean],
                     eventType:Option[String],
                     entryType:Option[String],
                     deceased:Option[Deceased],
                     latestReturn:Option[LatestReturn])

object RiskInput {
  implicit val formats: OFormat[RiskInput] = Json.format[RiskInput]

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
