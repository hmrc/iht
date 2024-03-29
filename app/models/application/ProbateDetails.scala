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

package models.application

import utils.CommonHelper
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads}

/**
  * Created by vineet on 05/07/17.
  */
case class ProbateDetails(grossEstateforIHTPurposes : BigDecimal,
                          grossEstateforProbatePurposes: BigDecimal,
                          totalDeductionsForProbatePurposes: BigDecimal,
                          netEstateForProbatePurposes: BigDecimal,
                          valueOfEstateOutsideOfTheUK: BigDecimal,
                          valueOfTaxPaid: BigDecimal,
                          probateReference: String)

object ProbateDetails {

  implicit val probateDetailsReads: Reads[ProbateDetails]= (
    (JsPath \ "grossEstateforIHTPurposes").read[BigDecimal] and
      (JsPath \ "grossEstateforProbatePurposes").read[BigDecimal] and
      (JsPath \ "totalDeductionsForProbatePurposes").read[BigDecimal] and
      (JsPath \ "netEstateForProbatePurposes").read[BigDecimal].map{CommonHelper.isProbateNetValueNegative} and
      (JsPath \ "valueOfEstateOutsideOfTheUK").read[BigDecimal] and
      (JsPath \ "valueOfTaxPaid").read[BigDecimal] and
      (JsPath \ "probateReference").read[String]
    )(ProbateDetails.apply _)

  implicit val formats = Json.format[ProbateDetails]
}
