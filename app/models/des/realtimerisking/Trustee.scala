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
  implicit val formats: OFormat[Trustee] = Json.format[Trustee]
}
