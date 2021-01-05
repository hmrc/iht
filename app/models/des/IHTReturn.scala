/*
 * Copyright 2021 HM Revenue & Customs
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

package models.des

import models.application.ApplicationDetails
import play.api.libs.json.Json
import org.joda.time.LocalDate
import constants.Constants
import org.joda.time.LocalDateTime
import utils.des.IhtReturnHelper

case class IHTReturn(acknowledgmentReference: Option[String]=None,
                     submitter: Option[Submitter]=None,
                     deceased: Option[Deceased]=None,
                     freeEstate: Option[FreeEstate]=None,
                     gifts: Option[Set[Set[Gift]]]=None,
                     trusts: Option[Set[Trust]]=None,
                     declaration: Option[Declaration]=None)

object IHTReturn {
  implicit val formats = Json.format[IHTReturn]
  def fromApplicationDetails(ad:ApplicationDetails,
                             declarationDate:LocalDateTime,
                             acknowledgmentReference: String,
                             dateOfDeath:LocalDate) :IHTReturn = {

    val kickoutReason = ad.kickoutReason.getOrElse("")
    if (kickoutReason.length>0) {
      throw new RuntimeException("Application is kicked out with a kickoutReason of " + kickoutReason + ". The application details object is: " + ad.toString())
    }

    IHTReturn(Some(acknowledgmentReference),
      submitter=Some(Submitter(submitterRole=Some(Constants.IHTReturnSubmitterRole))),
      deceased=Some(IhtReturnHelper.buildDeceased(ad, dateOfDeath)),
      freeEstate=IhtReturnHelper.buildFreeEstate(ad),
      gifts=IhtReturnHelper.buildGifts(ad, dateOfDeath),
      trusts=IhtReturnHelper.buildTrusts(ad),
      declaration=Some(IhtReturnHelper.buildDeclaration(ad, declarationDate)))
  }
}
