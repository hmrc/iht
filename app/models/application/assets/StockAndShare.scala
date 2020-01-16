/*
 * Copyright 2020 HM Revenue & Customs
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

package models.application.assets

import models.application.basicElements.EstateElement
import play.api.libs.json.Json

/**
  * Created by vineet on 03/11/16.
  */
case class StockAndShare(valueNotListed: Option[BigDecimal],
                         valueListed: Option[BigDecimal],
                         value: Option[BigDecimal],
                         isNotListed: Option[Boolean]= None,
                         isListed: Option[Boolean] = None) extends EstateElement

object StockAndShare {
  implicit val formats = Json.format[StockAndShare]
}
