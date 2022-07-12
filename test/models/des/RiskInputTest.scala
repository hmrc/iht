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

import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.play.PlaySpec
import utils.CommonBuilder

class RiskInputTest extends PlaySpec {
  "RiskInput" must {
    val acknowledgementReference = "acknowledgement"

    "successfully convert a valid IHT model to a valid DES RiskInput model" in {
      val ir1 = buildRiskInputCorrespondingToRegistrationDetailsAllFields(acknowledgementReference)
      val ir2 = RiskInput.fromRegistrationDetails(
        CommonBuilder.buildRegistrationDetailsDODandDeceasedDetails,
        acknowledgementReference)

      ir1.acknowledgementReference mustBe ir2.acknowledgementReference
      ir1.deceased mustBe ir2.deceased
      ir1.entryType mustBe ir2.entryType
      ir1.eventType mustBe ir2.eventType
      ir1.latestReturn mustBe ir2.latestReturn
    }
  }



  def buildRiskInputCorrespondingToRegistrationDetailsAllFields(acknowledgementReference: String) = {
    RiskInput(
      acknowledgementReference=Some(acknowledgementReference),
      riskConsidered=None,
      eventType=Some("death"),
      entryType=Some("Free Estate"),
      deceased=Some(buildDeceased),
      latestReturn=Some(buildLatestReturn)
    )
  }

  private def buildLatestReturn = {
    val asset = Asset(
      assetCode= Some("9001"),
      assetDescription= Some("Rolled up bank and building society accounts"),
      assetID= Some("null"),
      assetTotalValue= Some(BigDecimal(0)))
    LatestReturn(
      acknowledgementReference=None,
      freeEstate=Some(FreeEstate(estateAssets=Some(Set(asset))))
    )
  }

  private def buildDeceased = {
    import constants.Constants
    Deceased(
      title= None,
      firstName= Some("XYZAB"),
      middleName= None,
      lastName= Some("ABCXY"),
      dateOfBirth= Some("1998-12-12"),
      gender= None,
      nino= None,
      utr= None,
      personId=None,
      mainAddress= Some(new Address(
        Some("addr1"),
        Some("addr2"),
        None,
        None,
        Some("AA1 1AA"),
        Some("GB")
      )),
      dateOfDeath = Some("1987-12-12"),
      domicile=Some(Constants.IHTReturnDummyDomicile),
      otherDomicile=None,
      occupation=None,
      maritalStatus=Some(Constants.MaritalStatusMarried)
    )
  }

}
