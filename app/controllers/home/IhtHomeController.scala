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

package controllers.home

import com.github.fge.jsonschema.core.report.ProcessingReport
import constants.Constants
import json.JsonValidator
import metrics.Metrics
import models.{RegistrationDetails, IhtApplication}
import connectors.IHTConnector
import org.joda.time.LocalDate
import play.api.mvc.Action
import uk.gov.hmrc.play.http.Upstream4xxResponse
import uk.gov.hmrc.play.microservice.controller.BaseController
import play.api.libs.json._
import utils.ControllerHelper._
import utils._
import play.api.{Logger, Play}
import scala.concurrent.ExecutionContext.Implicits.global
import models.RegistrationDetails.registrationDetailsReads
import models.enums._

import scala.concurrent.Future


/**
 * Created by jon on 19/06/15.
 */
object IhtHomeController extends IhtHomeController {
  val ihtConnector = IHTConnector
  def metrics: Metrics = Metrics
}

trait IhtHomeController extends BaseController {
  val ihtConnector: IHTConnector
  def metrics: Metrics

  def listCases(nino: String) = Action.async {
    implicit request => ControllerHelper.exceptionCheckForResponses ({
      ihtConnector.getCaseList(nino).map {
        httpResponse => httpResponse.status match {
          case OK => {
            Logger.info("List Cases Response")
            metrics.incrementSuccessCounter(Api.GET_CASE_LIST)

            val js:JsValue = Json.parse(httpResponse.body)
            val pr:ProcessingReport = JsonValidator.validate(js, Constants.schemaPathListCases)
            if (pr.isSuccess) {
              try {
                Ok(Json.toJson(processResponse(js))).as("text/json")
              } catch {
                case e: Exception => throw new Upstream4xxResponse("Empty case return", NOT_FOUND, NOT_FOUND)
              }
            } else {
              processJsonValidationError(pr, js)
            }
          }
          case NO_CONTENT => {
            Logger.info("List cases returned No Content")
            NoContent
          }
          case ( _ ) => {
            Logger.info("List cases failured to work")
            InternalServerError
          }
        }
      }
    },Api.GET_CASE_LIST)
  }


  def processResponse(js:JsValue): Seq[IhtApplication] = {
    (js \ "deathEvents").asInstanceOf[JsArray].value.map(createIhtApplication(_))

  }

  def createIhtApplication(js:JsValue):IhtApplication = {

    val ihtRef =                  js \\ "ihtReference" head
    val firstName =               js \\ "firstName" head
    val lastName =                js \\ "lastName" head
    val dateOfBirth =             js \\ "dateOfBirth" head
    val dateOfDeath =             js \\ "dateOfDeath" head

    // The NINO is optional - there may be no NINO in the response,
    // hence if not found we should not throw an exception but instead
    // just store an empty string.
    val ninoSeq:Seq[JsValue] =    js \\ "nino"
    val ninoAsString:String =     if(ninoSeq.isEmpty) "" else ninoSeq.head.as[String]

    val entryType=                js \\ "entryType" head
    val role =                    js \\ "roleOfSubject" head
    val registrationDate=         js \\ "registrationDate" head
    val currentStatus =           js \\ "status" head
    val acknowledgmentReference = js \\ "acknowledgmentReference" head

    Logger.debug("List Cases acknowledgement " + acknowledgmentReference.toString())

    IhtApplication(ihtRef.as[String],
      firstName.as[String],
      lastName.as[String],
      dateOfBirth
        .as[LocalDate],
      dateOfDeath.as[LocalDate],
      ninoAsString,
      entryType.as[String],
      role.as[String],
      registrationDate.as[LocalDate],
      currentStatus.as[String],
      acknowledgmentReference.as[String])

  }

  /*
  * Fetch the case Details fro DES for the given nino and Iht Reference
  */
  def caseDetails(nino:String,ihtReference:String) = Action.async {

    implicit request => ControllerHelper.exceptionCheckForResponses ({
      ihtConnector.getCaseDetails(nino,ihtReference).map {
      httpResponse => httpResponse.status match {
        case OK => {
          Logger.debug("getCase Details response")
          metrics.incrementSuccessCounter(Api.GET_CASE_DETAILS)
          val js:JsValue = Json.parse(httpResponse.body)
          val pr:ProcessingReport = JsonValidator.validate(js, Constants.schemaPathCaseDetails)

          if (pr.isSuccess) {
            Logger.info("DES Response Validated")
            Logger.info("Get Case Details Acknowledgment Ref: " + httpResponse.json.\("acknowledgmentReference"))

            val registrationDetails: RegistrationDetails = Json.fromJson(js)(registrationDetailsReads) match {
              case JsSuccess(x, js) => {
                Logger.info("Successful return on getCaseDetails")
                x
              }
              case JsError(e) => {
                Logger.error("Failure to get correct")
                throw new RuntimeException(e.toString())
              }
            }
            Ok(Json.toJson(registrationDetails)).as("text/json")
          } else {
           processJsonValidationError(pr, js)
          }
        }
        case NO_CONTENT => {
          NoContent
        }
        case ( _ ) => {
          InternalServerError
        }
      }
    }
    },Api.GET_CASE_DETAILS)
  }
}
