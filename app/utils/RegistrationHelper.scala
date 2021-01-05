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

package utils

import connectors.IhtConnector
import javax.inject.Inject
import metrics.MicroserviceMetrics
import models.registration.RegistrationDetails

import scala.concurrent.ExecutionContext.Implicits.global
import models.enums._

class RegistrationHelperImpl @Inject()(val ihtConnector: IhtConnector,
                                       val metrics: MicroserviceMetrics) extends RegistrationHelper

trait RegistrationHelper extends ControllerHelper {
  val ihtConnector: IhtConnector
  /*
  * Fetch the registration Details from DES for the given nino and Iht Reference
  */
  def getRegistrationDetails(nino:String,ihtReference:String):Option[RegistrationDetails] = {
    import play.api.http.Status._
    import RegistrationDetails.registrationDetailsReads
    import play.api.Logger
    import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

    import scala.concurrent.Await
    import scala.concurrent.duration._

    val rd = exceptionCheckForResponses ({
      ihtConnector.getCaseDetails(nino,ihtReference).map {
        httpResponse => httpResponse.status match {
          case OK => {
            Logger.debug("\n getCase Details response" + Json.prettyPrint(httpResponse.json))
            val js:JsValue = Json.parse(httpResponse.body)
            Json.fromJson(js)(registrationDetailsReads)  match {
              case JsSuccess(x,js) => Some(x)
              case JsError(e) => {
                Logger.error(e.toString())
                throw new RuntimeException(e.toString())
              }
            }
          }
          case NO_CONTENT => {
            None
          }
          case ( _ ) => {
            None
          }
        }
      }
    },Api.GET_CASE_DETAILS)
    Await.result(rd, Duration.Inf)
  }
}
