/*
 * Copyright 2017 HM Revenue & Customs
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

package models

import constants.Constants
import org.joda.time.{LocalDateTime, DateTime, LocalTime, LocalDate}
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads}

/**
 * Created by yasar on 2/4/15.
 */
case class RegistrationDetails(deceasedDateOfDeath: Option[DeceasedDateOfDeath], applicantDetails:
Option[ApplicantDetails], deceasedDetails: Option[DeceasedDetails],
                               coExecutors: Seq[CoExecutor] = Seq(),
                               ihtReference: Option[String] = None,
                               status: String = Constants.AppStatusAwaitingReturn,
                               acknowledgmentReference:String = "",
                                returns: Seq[ReturnDetails] = Seq())


object RegistrationDetails {

  implicit val contactDetailsReads: Reads[ContactDetails] = (
    (JsPath \ "phoneNumber").readNullable[String] and
    (JsPath \ "eMailAddress").readNullable[String]
    )((phoneNumber,
      emailAddress)=>ContactDetails.apply(phoneNumber.getOrElse(""),emailAddress.getOrElse("") ))

  implicit val ukAddressReads: Reads[UkAddress]=(
    (JsPath \ "addressLine1").read[String] and
      (JsPath \ "addressLine2").read[String] and
      (JsPath \ "addressLine3").readNullable[String] and
      (JsPath \ "addressLine4").readNullable[String] and
      (JsPath \ "postalCode").readNullable[String] and
      (JsPath \ "countryCode").read[String]
    )(
    (addLine1, addLine2, addLine3, addLine4, postalCode, countryCode) => UkAddress.apply(
      addLine1, addLine2, addLine3, addLine4, postalCode.getOrElse(""), countryCode)
  )

  implicit val applicantDetailsReads: Reads[ApplicantDetails] = (
      (JsPath \ "firstName").read[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "nino").readNullable[String] and
      (JsPath \ "utr").readNullable[String] and
      (JsPath \ "gender").readNullable[String] and
      (JsPath \ "dateOfBirth").read[LocalDate] and
      (JsPath \ "mainAddress").read[UkAddress] and
      (JsPath \ "contactDetails").readNullable[ContactDetails]
    )((firstName,middleName,lastName,nino,utr,gender,dateOfBirth,mainAddress,contactDetails) => ApplicantDetails.apply(
    firstName,
    Some(middleName.getOrElse("")),
    lastName,
    nino.getOrElse(""),
    Some(utr.getOrElse("")),
    dateOfBirth,
    mainAddress,
    contactDetails.map(_.phoneNo),
    country=Constants.ApplicantCountryEnglandOrWales,
    role=Constants.RoleLeadExecutor))

  implicit val deceasedDetailsReads : Reads[DeceasedDetails]= (
    (JsPath \  "firstName").read[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "nino").readNullable[String] and
      (JsPath \ "dateOfBirth").read[LocalDate] and
      (JsPath \ "mainAddress").read[UkAddress] and
      (JsPath \ "domicile").read[String] and
      (JsPath \ "maritalStatus").read[String]
    )((firstName,middleName,lastName,nino,dateOfBirth,mainAddress,domicile,maritalStatus)=> DeceasedDetails.apply(
    firstName,
    Some(middleName.getOrElse("")),
    lastName,
    nino.getOrElse(""),
    mainAddress,
    utr=None,
    dateOfBirth,
    domicile,
    maritalStatus
  ))

  implicit val coExecutorReads: Reads[CoExecutor]= (
    (JsPath \  "firstName").read[String] and
      (JsPath \ "middleName").readNullable[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "dateOfBirth").read[LocalDate] and
      (JsPath \ "nino").readNullable[String] and
      (JsPath \ "utr").readNullable[String] and
      (JsPath \ "gender").readNullable[String] and
      (JsPath \ "mainAddress").read[UkAddress] and
      (JsPath \ "contactDetails").readNullable[ContactDetails]
    )((firstName,middleName,lastName,dateOfBirth,nino,utr,gender,mainAddress,contactDetails)=>CoExecutor.apply(
    id=None,
    firstName,
    Some(middleName.getOrElse("")),
    lastName,
    dateOfBirth,
    nino.getOrElse(""),
    Some(utr.getOrElse("")),
    mainAddress,
    contactDetails.getOrElse(new ContactDetails("","")),
    role= Some(Constants.RoleExecutor)
  ))

  implicit val returnDetailsReads: Reads[ReturnDetails]= (
    (JsPath \ "returnDate").readNullable[String] and
    (JsPath \ "returnId").readNullable[String] and
    (JsPath \ "returnVersionNumber").readNullable[String] and
    (JsPath \ "submitterRole").read[String]
    )(ReturnDetails.apply _)

  implicit val registrationDetailsReads: Reads[RegistrationDetails]=(
    (JsPath \ "deceased" \ "dateOfDeath").read[Option[LocalDate]].map(_.map(DeceasedDateOfDeath(_))) and
      (JsPath \ "leadExecutor").read[Option[ApplicantDetails]] and
      (JsPath \ "deceased" ).read[Option[DeceasedDetails]] and
      (JsPath \ "coExecutors").readNullable[Seq[CoExecutor]].map{_.getOrElse(Seq())} and
      (JsPath \ "ihtReference").read[Option[String]] and
      (JsPath \ "caseStatus").read[String] and
      (JsPath \ "acknowledgmentReference").read[String] and
      (JsPath \ "returns").read[Seq[ReturnDetails]]
    )(RegistrationDetails.apply _)

 implicit val formats = Json.format[RegistrationDetails]

}
