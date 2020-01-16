/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}

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
  implicit val formats: OFormat[Gift] = Json.format[Gift]
}
