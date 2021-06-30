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

package connectors

import constants.Constants
import constants.Constants._

import javax.inject.Inject
import metrics.MicroserviceMetrics
import models.enums._
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import services.AuditService
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.HeaderDecorator
import utils.exception.DESInternalServerError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class IhtConnectorImpl @Inject()(val metrics: MicroserviceMetrics,
                                 val http: DefaultHttpClient,
                                 val auditService: AuditService,
                                 val headerDecorator: HeaderDecorator,
                                 val servicesConfig: ServicesConfig) extends IhtConnector {
  override val serviceURL: String = servicesConfig.baseUrl("iht")
  override val urlHeaderEnvironment = headerDecorator.urlHeaderEnvironmentValue
  override val urlHeaderAuthorization = headerDecorator.decoratedAuthorization(headerDecorator.urlHeaderAuthorizationValue)
}

trait IhtConnector extends Logging {

  val serviceURL: String
  val urlHeaderEnvironment: String
  val urlHeaderAuthorization: String
  val http: DefaultHttpClient
  val auditService: AuditService
  val headerDecorator: HeaderDecorator

  def metrics: MicroserviceMetrics

  private def createHeaderCarrier =  HeaderCarrier()

  def submitRegistration(nino: String, registrationJs: JsValue)(implicit request: Request[_]): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier

    /*
   * Recover cases are written because of the framework design.
   * Any HttpResponse that does NOT have 2xx status code is wrapped into a different kind of exception by framework.
   */
    logger.info("Start Submitting registration process, creating metrics ")

    val timerContext = metrics.startTimer(Api.SUB_REGISTRATION)
    val urlToRead = s"$serviceURL/inheritance-tax/individuals/$nino/cases/"

    logger.info("Submitting registration to DES")
    val futureResponse: Future[HttpResponse] = http.POST(urlToRead, registrationJs, headerDecorator.desExternalHttpHeaders())
    /* Code to replicate a failure due to duplicated submission.
       Uncomment to replace above line when testing required:-
      Future.failed[HttpResponse](Upstream4xxResponse("test", 409, 409))
    */

    futureResponse.map {
      response => {
        logger.info("Received response from DES")
        auditService.auditRequestWithResponse(urlToRead, "POST", Json.stringify(registrationJs), futureResponse)
        timerContext.stop()
        response
      }
    } recoverWith {
      case e: Exception =>
        logger.info("Exception occured while registration submission ::: " + e.getMessage)
        val keyMap = Map("request" -> RegSubmissionRequestKey, "response" -> RegSubmissionFailureResponseKey)
        auditSubmissionFailure(registrationJs, futureResponse, keyMap, Constants.AuditTypeIHTRegistrationSubmitted)
        Future.failed(throw e)
    }
  }

  implicit val readApiResponse: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse): HttpResponse = IhtResponseHandler.handleIhtResponse(method, url, response) match {
      case Right(x) => x
      case Left(x) => throw UpstreamErrorResponse.apply(x.getMessage(), x.statusCode, x.reportAs, x.headers)
    }
  }

  def submitApplication(nino: String, ihtRef: String, applicationJs: JsValue)(implicit request: Request[_]): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier

    logger.info("Start Submitting application process, creating metrics ")
    val timerContext = metrics.startTimer(Api.SUB_APPLICATION)
    val urlToRead = s"$serviceURL/inheritance-tax/individuals/$nino/cases/$ihtRef/returns"

    logger.info("Submit application to DES")
    val futureResponse = http.POST(urlToRead, applicationJs, headerDecorator.desExternalHttpHeaders())
    futureResponse map {
      response => {
        logger.info("Received response from DES")
        auditService.auditRequestWithResponse(urlToRead, "POST", Json.stringify(applicationJs), futureResponse)
        timerContext.stop()
        response
      }
    } recoverWith {
      case e: Exception =>
        logger.info("Exception occured while application submission ::: " + e.getMessage)
        val keyMap = Map("request" -> AppSubmissionRequestKey, "response" -> AppSubmissionFailureResponseKey)
        auditSubmissionFailure(applicationJs, futureResponse, keyMap, Constants.AuditTypeIHTEstateReportSubmitted)
        Future.failed(throw e)
    }
  }

  def submitRealtimeRisking(ihtReference: String, ackRef: String, realtimeRiskingJs: JsValue): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    logger.info("Submit risking info to DES")
    val timerContext = metrics.startTimer(Api.SUB_REAL_TIME_RISKING)
    http.POST(s"$serviceURL/risk-score/inheritance-tax/$ihtReference?ackRef=$ackRef",
      realtimeRiskingJs, headerDecorator.desExternalHttpHeaders()) map {
      response => {
        timerContext.stop()
        logger.info(s"${response.status} returned from submission of risking info to DES")
        response
      }
    }
  }

  /*
   * Fetch the CaseList data from Des (As os now getting from Stub)for given nino
   */
  def getCaseList(nino: String): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    logger.info("Get case list from DES")
    val timerContext = metrics.startTimer(Api.GET_CASE_LIST)
    http.GET(s"$serviceURL/inheritance-tax/individuals/$nino/cases/",
      headers = headerDecorator.desExternalHttpHeaders()) map {
      response => {
        timerContext.stop()
        logger.info(s"${response.status} returned when getting Case List")
        response
      }
    }
  }

  /*
   * Fetch the case Details fro DES for the given nino and Iht Reference
   */
  def getCaseDetails(nino: String, ihtReference: String): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    logger.info("Get case details from DES")
    val timerContext = metrics.startTimer(Api.GET_CASE_DETAILS)
    http.GET(s"$serviceURL/inheritance-tax/individuals/$nino/cases/$ihtReference",
      headers = headerDecorator.desExternalHttpHeaders()) map {
      response => {
        timerContext.stop()
        logger.info(s"${response.status} returned from Getting Case Details")
        response
      }
    }
  }

  def requestClearance(nino: String, ihtReference: String, requestJs: JsValue): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    logger.info("Request clearance from DES")
    val timerContext = metrics.startTimer(Api.SUB_REQUEST_CLEARANCE)
    http.POST(s"$serviceURL/inheritance-tax/individuals/$nino/cases/$ihtReference/clearance",
      requestJs, headerDecorator.desExternalHttpHeaders()) map {
      response => {
        timerContext.stop()
        logger.info(s"${response.status} returned from submitting the Request Clearance")
        response
      }
    }
  }

  /*
   * Fetch the Probate Details for given nino,ihtRef and returnId
   */
  def getProbateDetails(nino: String, ihtReference: String, ihtReturnId: String): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    logger.info("Get Probate Details from DES")
    val timerContext = metrics.startTimer(Api.GET_PROBATE_DETAILS)
    http.GET(
      s"$serviceURL/inheritance-tax/individuals/$nino/cases/$ihtReference/returns/$ihtReturnId/probate",
      headers = headerDecorator.desExternalHttpHeaders()) map {
      response => {
        timerContext.stop()
        logger.info(s"${response.status} returned from getting Probate details")
        response
      }
    }
  }

  def getSubmittedApplicationDetails(nino: String, ihtReference: String, returnId: String): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    logger.info("Get submitted application details from DES")
    val timerContext = metrics.startTimer(Api.SUB_APPLICATION)
    http.GET(
      s"$serviceURL/inheritance-tax/individuals/$nino/cases/$ihtReference/returns/$returnId",
      headers = headerDecorator.desExternalHttpHeaders()) map {
      response => {
        timerContext.stop()
        logger.info(s"${response.status} returned from submitting the application")
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
  private def auditSubmissionFailure(requestJs: JsValue,
                                     responseToAudit: Future[HttpResponse],
                                     keys: Map[String, String],
                                     transactionName: String)(implicit request: Request[_]): Unit = {
    implicit val hc: HeaderCarrier = createHeaderCarrier

    responseToAudit.onComplete[Any] {
      case Failure(t) => {
        auditService.sendSubmissionFailureEvent(Map(keys("request") -> requestJs.toString(),
          keys("response") -> t.getMessage), transactionName)
      }
      case Success(_) =>
    }
  }

}

object IhtResponseHandler extends IhtResponseHandler

trait IhtResponseHandler extends HttpErrorFunctions {
  def handleIhtResponse(method: String, url: String, response: HttpResponse): Either[UpstreamErrorResponse, HttpResponse] = {
    response.status match {
      case x if is5xx(response.status) =>
        throw DESInternalServerError(UpstreamErrorResponse.apply(upstreamResponseMessage(method, url, response.status, response.body), response.status, 502))
      case _ => handleResponseEither(method, url)(response)
    }
  }
}