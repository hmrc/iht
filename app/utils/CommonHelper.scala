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

package utils

import java.util.UUID._
import constants.Constants
import org.joda.time.{LocalDate, LocalDateTime}
import play.api.Logger

import scala.concurrent.Future

object CommonHelper {
  def generateAcknowledgeReference:String=
    randomUUID.toString().replaceAll("-", "").toUpperCase

  def dateTimeToDesString(dt: LocalDateTime) = {
    val s = dt.toString().substring(0,19)
    s
  }

  def dateToDesString( ld: LocalDate ) = {
    ld.toString(Constants.IHTReturnDateFormat)
  }

  /**
   * Converts date from format "1 January 2015" to "2015-01-01"
   * @param date
   * @return
   */
  def dateLongFormatToDesString(date:String):String = {
    import java.util.StringTokenizer
    val st = new StringTokenizer(date, " ")
    if (st.countTokens() > 2) {
      val day = last2("0" + st.nextToken())
      val month = st.nextToken()
      val year = st.nextToken()
      val month2 = last2(Constants.monthNameToNumber.get(month) match {
        case None => throw new RuntimeException("Invalid month: " + month)
        case Some(m) => "0" + m
      })
      year + "-" + month2 + "-" + day
    } else {
      throw new RuntimeException("Invalid date format: " + date)
    }
  }

  /**
    * Converts date from format "2015-4-5" to "2015-04-05"
    *
    * @param date
    * @return
    */
  def dateFormatChangeToPadZeroToDayAndMonth(date: String): String = {
    import java.util.StringTokenizer
    val st = new StringTokenizer(date, "-")

    if (st.countTokens() > 2) {
      val year = st.nextToken()
      val month = st.nextToken().toInt
      val day = st.nextToken().toInt

      val updatedMonthValue = if (month < 10) "0" + month else month
      val updatedDayValue = if (day < 10) "0" + day else day

      year + "-" + updatedMonthValue + "-" + updatedDayValue
    } else {
      throw new RuntimeException("Invalid date format: " + date)
    }
  }

  def last2(s:String):String = if (s.length > 1) s.substring(s.length - 2, s.length) else ""

  def isProbateNetValueNegative(value: BigDecimal): BigDecimal = {
    if (value < BigDecimal(0)) {
      BigDecimal(0)
    } else {
      value
    }
  }

  def getOrException[A](option: Option[A], errorMessage:String = "No element found"):A =
    option.fold(throw new RuntimeException(errorMessage))(identity)
}
