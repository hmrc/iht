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

package models.enums

/**
 *
 * Created by Vineet Tyagi on 25/09/15.
 *
 */
object Api extends Enumeration{

  type Api = Value

  val GET_CASE_LIST = Value
  val GET_CASE_DETAILS = Value
  val GET_PROBATE_DETAILS = Value
  val GET_APPLICATION_DETAILS = Value
  val SUB_REGISTRATION = Value
  val SUB_REAL_TIME_RISKING = Value
  val SUB_APPLICATION = Value
  val SUB_REQUEST_CLEARANCE = Value

}
