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

package models.application.tnrb

import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.libs.json.JodaReads._
import play.api.libs.json.JodaWrites._

/**
 * Created by Vineet Tyagi on 23/04/15.
 *
 * Model for TRNB Eligibility and associated with iht.forms.TnrbEligibilityForm
 *
 */
case class TnrbEligibiltyModel(isPartnerLivingInUk: Option[Boolean],
                               isGiftMadeBeforeDeath: Option[Boolean],
                               isStateClaimAnyBusiness: Option[Boolean],
                               isPartnerGiftWithResToOther: Option[Boolean],
                               isPartnerBenFromTrust: Option[Boolean],
                               isEstateBelowIhtThresholdApplied: Option[Boolean],
                               isJointAssetPassed: Option[Boolean],
                               firstName: Option[String],
                               lastName: Option[String],
                               dateOfMarriage: Option[LocalDate],
                               dateOfPreDeceased: Option[LocalDate])

object TnrbEligibiltyModel {
  implicit val formats = Json.format[TnrbEligibiltyModel]
}
