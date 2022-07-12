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

package models.application.debts

import play.api.libs.json.{Json, OFormat}

case class AllLiabilities(funeralExpenses: Option[BasicEstateElementLiabilities] = None,
                          trust: Option[BasicEstateElementLiabilities] = None,
                          debtsOutsideUk: Option[BasicEstateElementLiabilities] = None,
                          jointlyOwned: Option[BasicEstateElementLiabilities] = None,
                          other: Option[BasicEstateElementLiabilities] = None,
                          mortgages: Option[MortgageEstateElement] = None
                         ) {
  def totalValue(): BigDecimal = funeralExpenses.flatMap(_.value).getOrElse(BigDecimal(0)) +
    trust.flatMap(_.value).getOrElse(BigDecimal(0)) +
    debtsOutsideUk.flatMap(_.value).getOrElse(BigDecimal(0)) +
    jointlyOwned.flatMap(_.value).getOrElse(BigDecimal(0)) +
    other.flatMap(_.value).getOrElse(BigDecimal(0)) +
    mortgageValue

  def mortgageValue: BigDecimal = {
    val mort = mortgages.getOrElse(new MortgageEstateElement(Some(false), Nil)).mortgageList

    mort match {
      case x : List[Mortgage] if x.nonEmpty => x.flatMap(_.value).sum
      case _                                => BigDecimal(0)
    }
  }
}

object AllLiabilities {
  implicit val formats: OFormat[AllLiabilities] = Json.format[AllLiabilities]
}
