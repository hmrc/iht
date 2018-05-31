/*
 * Copyright 2018 HM Revenue & Customs
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

package models.application.exemptions

import org.joda.time.LocalDate
import play.api.libs.json.Json

/**
  * Created by vineet on 05/07/17.
  */
case class PartnerExemption(
                             isAssetForDeceasedPartner: Option[Boolean],
                             isPartnerHomeInUK: Option[Boolean],
                             firstName: Option[String],
                             lastName: Option[String],
                             dateOfBirth: Option[LocalDate],
                             nino: Option[String],
                             totalAssets: Option[BigDecimal])

object PartnerExemption {
  implicit val formats = Json.format[PartnerExemption]
}
