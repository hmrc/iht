/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.registration

import connectors.IhtConnector
import constants.Constants
import json.JsonValidator
import metrics.Metrics
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Result}
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.microservice.controller.BaseController
import utils.ControllerHelper._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import models.enums._
import services.AuditService
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.exception.DESInternalServerError

/**
  * Created by yasar on 2/5/15.
  */
object RegistrationController extends RegistrationController {
  val desConnector = IhtConnector

  override def metrics: Metrics = Metrics

  override def auditService = AuditService
}

trait RegistrationController extends BaseController {
  val desConnector: IhtConnector

  def metrics: Metrics = Metrics

  def auditService: AuditService

  def recoverOnSubmit: PartialFunction[Throwable, Future[Result]] = {
    case Upstream4xxResponse(message, CONFLICT, _, _) =>
      metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
      Logger.warn(s"Received 409 from DES - converting to 202. Details:- $message")
      Future.successful(Accepted(message))
    case e:GatewayTimeoutException =>
      Logger.warn("Gateway Timeout Response Returned ::: " + e.getMessage)
      Future.failed(new GatewayTimeoutException(e.message))
    case e: BadRequestException =>
      Logger.warn("BadRequest Response Returned ::: " + e.getMessage)
      metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
      Future.failed(new BadRequestException(e.message))
    case e: Upstream4xxResponse =>
      Logger.info(" Upstream4xxResponse Returned ::: " + e.getMessage)
      metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
      Future.failed(new Upstream4xxResponse(e.message, e.upstreamResponseCode, e.reportAs))
    case e: Upstream5xxResponse =>
      Logger.info("Upstream5xxResponse Returned ::: " + e.getMessage)
      metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
      Future.failed(new Upstream5xxResponse(e.message, e.upstreamResponseCode, e.reportAs))
    case e: NotFoundException =>
      Logger.info("Upstream4xxResponse Returned ::: " + e.getMessage)
      metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
      Future.failed(new Upstream4xxResponse(e.message, NOT_FOUND, NOT_FOUND))
    case e: DESInternalServerError =>
      metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
      throw e
    case e: Exception =>
      Logger.info("Exception Returned ::: " + e.getMessage)
      metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
      Future.failed(new Exception(e.getMessage))
  }

  def submit(nino: String): Action[JsValue] = Action.async(parse.json) {
    implicit request => {
      Logger.debug("RegistrationController submit request initiated")
      val pr = JsonValidator.validate(request.body, Constants.schemaPathRegistrationSubmission)
      if (pr.isSuccess) {
        Logger.debug("DES Request Validated")
        Logger.info("Acknowledgment Ref: " + request.body.\("acknowledgmentReference"))
        desConnector.submitRegistration(nino, request.body).flatMap {
          httpResponse =>
            (Json.parse(httpResponse.body) \ "referenceNumber").asOpt[String] match {
              case Some(ihtRef) =>
                Logger.info("Parsed IHT Reference from response")
                metrics.incrementSuccessCounter(Api.SUB_REGISTRATION)
                val jsonValue = request.body
                auditService.sendEvent(Constants.AuditTypeIHTRegistrationSubmitted,
                  jsonValue,
                  Constants.AuditTypeIHTRegistrationSubmittedTransactionName).map { auditResult =>
                  Logger.debug("http response status code " + httpResponse.status)
                  Logger.debug("Response " + Json.prettyPrint(httpResponse.json))
                  Ok(ihtRef)
                }
              case None =>
                Logger.info("Failure to parse IHTREF from response")
                Future.successful(InternalServerError("CAN NOT PARSE IHT REF FROM RESPONSE "))
            }
        }
      } else {
        Future(processJsonValidationError(pr, request.body))
      }
    } recoverWith recoverOnSubmit
  }
}
