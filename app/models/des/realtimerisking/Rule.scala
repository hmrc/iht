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

import play.api.libs.json.{Json, OFormat}

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
  implicit val formats: OFormat[Rule] = Json.format[Rule]
}
