/*
 * Copyright 2019 HM Revenue & Customs
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

package models.registration

import constants.Constants
import models.{ContactDetails, UkAddress}
import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.libs.json.JodaReads._
import play.api.libs.json.JodaWrites._

/**
  * Created by yasar on 3/10/15.
  */

case class ApplicantDetails(firstName: String,
                            middleName: Option[String],
                            lastName: String,
                            nino: String,
                            dateOfBirth: LocalDate,
                            ukAddress: UkAddress,
                            phoneNo: Option[String] = None,
                            country: String = Constants.ApplicantCountryEnglandOrWales,
                            role: String = Constants.RoleLeadExecutor)


object ApplicantDetails {
  implicit val formats = Json.format[ApplicantDetails]
}

case class DeceasedDetails(firstName: String,
                           middleName: Option[String],
                           lastName: String,
                           nino: String,
                           ukAddress: UkAddress,
                           dateOfBirth: LocalDate,
                           domicile: String,
                           maritalStatus: String)

object DeceasedDetails {
  implicit val formats = Json.format[DeceasedDetails]
}

case class DeceasedDateOfDeath(dateOfDeath: LocalDate)

object DeceasedDateOfDeath {
  implicit val formats = Json.format[DeceasedDateOfDeath]
}

//Classes for Add/Edit/Delete CoExecutors Starts

case class CoExecutor(id: Option[String],
                      firstName: String,
                      middleName: Option[String],
                      lastName: String,
                      dateOfBirth: LocalDate,
                      nino: String,
                      ukAddress: UkAddress,
                      contactDetails: ContactDetails,
                      role: Option[String] = Some(Constants.RoleExecutor)) {

  val name = firstName.capitalize + " " + lastName.capitalize
}

object CoExecutor {
  implicit val formats = Json.format[CoExecutor]
}

//Model for Return Details from E.T.M.P.

case class ReturnDetails(returnDate: Option[String],
                         returnId: Option[String],
                         returnVersionNumber: Option[String],
                         submitterRole: String)

object ReturnDetails {
  implicit val formats = Json.format[ReturnDetails]
}
