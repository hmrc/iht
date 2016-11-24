/*
 * Copyright 2016 HM Revenue & Customs
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

import play.api.libs.json.Json

/**
 * Created by yasar on 2/4/15.
 */
case class UkAddress(ukAddressLine1:String,
                     ukAddressLine2:String,
                     ukAddressLine3:Option[String],
                     ukAddressLine4:Option[String],
                     postCode:String,
                     countryCode:String)

object UkAddress{
  implicit val formats = Json.format[UkAddress]
}

case class ContactDetails(phoneNo: String, email: String)

object ContactDetails {
  implicit val formats = Json.format[ContactDetails]
}
