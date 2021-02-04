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

package controllers.estateReports

import com.github.fge.jsonschema.core.report.ProcessingReport
import connectors.IhtConnector
import constants.Constants
import javax.inject.Inject
import json.JsonValidator
import metrics.MicroserviceMetrics
import models.application.IhtApplication
import models.enums._
import models.registration.RegistrationDetails
import models.registration.RegistrationDetails.registrationDetailsReads
import org.joda.time.LocalDate
import play.api.libs.json.JodaReads._
import play.api.Logger.logger
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.{NotFoundException, Upstream4xxResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ControllerHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps


class YourEstateReportsControllerImpl @Inject()(val metrics: MicroserviceMetrics,
                                                val ihtConnector: IhtConnector,
                                                val cc: ControllerComponents) extends BackendController(cc) with YourEstateReportsController

trait YourEstateReportsController extends BackendController with ControllerHelper {
  val ihtConnector: IhtConnector
  val metrics: MicroserviceMetrics

  def listCases(nino: String): Action[AnyContent] = Action.async {
      exceptionCheckForResponses ({
      ihtConnector.getCaseList(nino).map {
        httpResponse => httpResponse.status match {
          case OK =>
            logger.info("List Cases Response")
            metrics.incrementSuccessCounter(Api.GET_CASE_LIST)

            val js:JsValue = Json.parse(httpResponse.body)
            val pr:ProcessingReport = JsonValidator.validate(js, Constants.schemaPathListCases)
            if (pr.isSuccess) {
              try {
                Ok(Json.toJson(processResponse(js))).as("text/json")
              } catch {
                case e: Exception => throw UpstreamErrorResponse.apply("Empty case return", NOT_FOUND, NOT_FOUND)
              }
            } else {
              processJsonValidationError(pr, js)
            }
          case NO_CONTENT =>
            logger.info("List cases returned No Content")
            NoContent
          case _ =>
            logger.info("List cases failed to work")
            InternalServerError
        }
      }
    } recover {
      case e: Upstream4xxResponse if e.upstreamResponseCode == NOT_FOUND =>
        logger.info("List cases returned Not Found")
        NoContent
      case _: NotFoundException =>
        logger.info("List cases returned Not Found")
        NoContent
    },Api.GET_CASE_LIST)
  }


  def processResponse(js:JsValue): Seq[IhtApplication] = {
    (js \ "deathEvents").as[JsArray].value.map(createIhtApplication)

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

    logger.debug("List Cases acknowledgement " + acknowledgmentReference.toString())

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
  def caseDetails(nino: String, ihtReference: String): Action[AnyContent] = Action.async {
      exceptionCheckForResponses ({
      ihtConnector.getCaseDetails(nino,ihtReference).map {
      httpResponse => httpResponse.status match {
        case OK =>
          logger.debug("getCase Details response")
          metrics.incrementSuccessCounter(Api.GET_CASE_DETAILS)
          val js:JsValue = Json.parse(httpResponse.body)
          val pr:ProcessingReport = JsonValidator.validate(js, Constants.schemaPathCaseDetails)

          if (pr.isSuccess) {
            logger.info("DES Response Validated")
            logger.info("Get Case Details Acknowledgment Ref: " + httpResponse.json.\("acknowledgmentReference"))

            val registrationDetails: RegistrationDetails = Json.fromJson(js)(registrationDetailsReads) match {
              case JsSuccess(value, _) =>
                logger.info("Successful return on getCaseDetails")
                value
              case JsError(e) =>
                logger.error("Failure to get correct")
                throw new RuntimeException(e.toString())
            }
            Ok(Json.toJson(registrationDetails)).as("text/json")
          } else {
           processJsonValidationError(pr, js)
          }
        case NO_CONTENT => NoContent
        case _          => InternalServerError
      }
    }
    },Api.GET_CASE_DETAILS)
  }
}
