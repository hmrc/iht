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

package connectors

import config.wiring.WSHttp
import constants.Constants
import metrics.Metrics
import play.api.libs.json.{JsValue, Json, Writes}
import services.AuditService
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import play.api.Logger
import play.api.mvc.{Action, Request}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import models.enums._
import constants.Constants._
import utils.CommonHelper._

import scala.util.Failure
import scala.util.Success
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization

trait IhtConnector {

  val serviceURL: String
  val urlHeaderEnvironment: String
  val urlHeaderAuthorization: String
  val http: HttpGet with HttpPost = WSHttp

  def metrics: Metrics

  private def createHeaderCarrier = HeaderCarrier(extraHeaders = Seq("Environment" -> urlHeaderEnvironment),
    authorization = Some(Authorization(urlHeaderAuthorization)))

  def submitRegistration(nino: String, registrationJs: JsValue)(implicit request:Request[_]): Future[HttpResponse] = {
    implicit val hc = createHeaderCarrier
    /*
   * Recover cases are written because of the framework design.
   * Any HttpResponse that does NOT have 2xx status code is wrapped into a different kind of exception by framework.
   */
    Logger.info("Start Submitting registration process, creating metrics ")

    val timerContext = metrics.startTimer(Api.SUB_REGISTRATION)
    val urlToRead = s"$serviceURL/inheritance-tax/individuals/$nino/cases/"

    Logger.info("Submitting registration to DES")
    val futureResponse: Future[HttpResponse] = http.POST(urlToRead, registrationJs)
    /* Code to replicate a failure due to duplicated submission.
       Uncomment to replace above line when testing required:-
      Future.failed[HttpResponse](Upstream4xxResponse("test", 409, 409))
    */

    futureResponse.map {
      response => {
        Logger.info("Received response from DES")
        AuditService.auditRequestWithResponse(urlToRead, "POST", Some(registrationJs), futureResponse)
        timerContext.stop()
        response
      }
    } recoverWith {
      case e: Exception => {
        Logger.info("Exception occured while registration submission ::: " + e.getMessage)
        val keyMap = Map("request" -> RegSubmissionRequestKey, "response" -> RegSubmissionFailureResponseKey)
        auditSubmissionFailure(registrationJs, futureResponse, keyMap, Constants.AuditTypeIHTRegistrationSubmitted)
        Future.failed(throw e)
      }
    }
  }

  def submitApplication(nino: String, ihtRef: String, applicationJs: JsValue)(implicit request:Request[_]): Future[HttpResponse] = {
    implicit val hc = createHeaderCarrier

    Logger.info("Start Submitting application process, creating metrics ")
    val timerContext = metrics.startTimer(Api.SUB_APPLICATION)
    val urlToRead = s"$serviceURL/inheritance-tax/individuals/$nino/cases/$ihtRef/returns"

    Logger.info("Submit application to DES")
    val futureResponse = http.POST(urlToRead, applicationJs)
    futureResponse map {
      response => {
        Logger.info("Received response from DES")
        AuditService.auditRequestWithResponse(urlToRead, "POST", Some(applicationJs), futureResponse)
        timerContext.stop()
        response
      }
    } recoverWith {
      case e: Exception => {
        Logger.info("Exception occured while application submission ::: " + e.getMessage)
        val keyMap = Map("request" -> AppSubmissionRequestKey, "response" -> AppSubmissionFailureResponseKey)
        auditSubmissionFailure(applicationJs, futureResponse, keyMap, Constants.AuditTypeIHTEstateReportSubmitted)
        Future.failed(throw e)
      }
    }
  }

  def submitRealtimeRisking(ihtReference: String, ackRef: String, realtimeRiskingJs: JsValue):
  Future[HttpResponse] = {
    implicit val hc = createHeaderCarrier
    Logger.info("Submit risking info to DES")
    val timerContext = metrics.startTimer(Api.SUB_REAL_TIME_RISKING)
    http.POST(s"$serviceURL/risk-score/inheritance-tax/$ihtReference?ackRef=$ackRef", realtimeRiskingJs) map {
      response => {
        timerContext.stop()
        Logger.info("Future httpResponse returned from submission of risking info to DES")
        response
      }
    }
  }

  /*
   * Fetch the CaseList data from Des (As os now getting from Stub)for given nino
   */
  def getCaseList(nino: String): Future[HttpResponse] = {
    implicit val hc = createHeaderCarrier
    Logger.info("Get case list from DES")
    val timerContext = metrics.startTimer(Api.GET_CASE_LIST)
    http.GET(s"$serviceURL/inheritance-tax/individuals/$nino/cases/") map {
      response => {
        timerContext.stop()
        Logger.info("Future httpResponse returned from Getting Case List")
        response
      }
    }
  }

  /*
   * Fetch the case Details fro DES for the given nino and Iht Reference
   */
  def getCaseDetails(nino: String, ihtReference: String): Future[HttpResponse] = {
    implicit val hc = createHeaderCarrier
    Logger.info("Get case details from DES")
    val timerContext = metrics.startTimer(Api.GET_CASE_DETAILS)
    http.GET(s"$serviceURL/inheritance-tax/individuals/$nino/cases/$ihtReference") map {
      response => {
        timerContext.stop()
        Logger.info("Future httpResponse returned from Getting Case Details")
        response
      }
    }
  }

  def requestClearance(nino: String, ihtReference: String, requestJs: JsValue): Future[HttpResponse] = {
    implicit val hc = createHeaderCarrier
    Logger.info("Request clearance from DES")
    val timerContext = metrics.startTimer(Api.SUB_REQUEST_CLEARANCE)
    http.POST(s"$serviceURL/inheritance-tax/individuals/$nino/cases/$ihtReference/clearance", requestJs) map {
      response => {
        timerContext.stop()
        Logger.info("Future httpResponse returned from submitting the Request Clearance")
        response
      }
    }
  }

  /*
   * Fetch the Probate Details for given nino,ihtRef and returnId
   */
  def getProbateDetails(nino: String, ihtReference: String, ihtReturnId: String): Future[HttpResponse] = {
    implicit val hc = createHeaderCarrier
    Logger.info("Get Probate Details from DES")
    val timerContext = metrics.startTimer(Api.GET_PROBATE_DETAILS)
    http.GET(s"$serviceURL/inheritance-tax/individuals/$nino/cases/$ihtReference/returns/$ihtReturnId/probate") map {
      response => {
        timerContext.stop()
        Logger.info("Future httpResponse returned from getting Probate details")
        response
      }
    }
  }

  def getSubmittedApplicationDetails(nino: String, ihtReference: String, returnId: String): Future[HttpResponse] = {
    implicit val hc = createHeaderCarrier
    Logger.info("Get submitted application details from DES")
    val timerContext = metrics.startTimer(Api.SUB_APPLICATION)
    http.GET(s"$serviceURL/inheritance-tax/individuals/$nino/cases/$ihtReference/returns/$returnId") map {
      response => {
        timerContext.stop()
        Logger.info("Future httpResponse returned from submitting the application")
        response
      }
    }
  }

  /**
    * Audit the submission failure event
    *
    * @param requestJs
    * @param responseToAudit
    */
  private def auditSubmissionFailure(requestJs: JsValue, responseToAudit: Future[HttpResponse], keys: Map[String, String], transactionName:String)(implicit request:Request[_]) = {
    implicit val hc = createHeaderCarrier

    responseToAudit.onComplete[Any] {
      case Failure(t) => {
        AuditService.sendSubmissionFailureEvent(Map(keys("request") -> requestJs.toString(),
          keys("response") -> t.getMessage), transactionName)
      }
      case Success(_) =>
    }
  }

}

object IhtConnector extends IhtConnector with ServicesConfig {
  override val serviceURL = baseUrl("iht")
  override val urlHeaderEnvironment = getOrException(config("iht").getString("des.environment"))
  override val urlHeaderAuthorization = s"Bearer ${getOrException(config("iht").getString("des.authorization-key"))}"

  override def metrics: Metrics = Metrics
}
