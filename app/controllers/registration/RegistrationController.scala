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

package controllers.registration

import connectors.IhtConnector
import constants.Constants
import javax.inject.Inject
import json.JsonValidator
import metrics.MicroserviceMetrics
import models.enums._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import services.AuditService
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ControllerHelper
import utils.exception.DESInternalServerError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationControllerImpl @Inject()(val desConnector: IhtConnector,
                                           val metrics: MicroserviceMetrics,
                                           val auditService: AuditService,
                                           val cc: ControllerComponents) extends BackendController(cc) with RegistrationController

trait RegistrationController extends BackendController with ControllerHelper {
  val desConnector: IhtConnector
  def metrics: MicroserviceMetrics
  def auditService: AuditService

  def recoverOnSubmit: PartialFunction[Throwable, Future[Result]] = {
    case Upstream4xxResponse(message, CONFLICT, _, _) =>
      metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
      logger.warn(s"Received 409 from DES - converting to 202. Details:- $message")
      Future.successful(Accepted(message))
    case e:GatewayTimeoutException =>
      logger.warn("Gateway Timeout Response Returned ::: " + e.getMessage)
      Future.failed(new GatewayTimeoutException(e.message))
    case e: BadRequestException =>
      logger.warn("BadRequest Response Returned ::: " + e.getMessage)
      metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
      Future.failed(new BadRequestException(e.message))
    case e: Upstream4xxResponse =>
      logger.info(" Upstream4xxResponse Returned ::: " + e.getMessage)
      metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
      Future.failed(UpstreamErrorResponse.apply(e.message, e.upstreamResponseCode, e.reportAs))
    case e: UpstreamErrorResponse =>
      logger.info("Upstream5xxResponse Returned ::: " + e.getMessage)
      metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
      Future.failed(UpstreamErrorResponse.apply(e.message, e.statusCode, e.reportAs))
    case e: NotFoundException =>
      logger.info("Upstream4xxResponse Returned ::: " + e.getMessage)
      metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
      Future.failed(UpstreamErrorResponse.apply(e.message, NOT_FOUND, NOT_FOUND))
    case e: DESInternalServerError =>
      metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
      throw e
    case e: Exception =>
      logger.info("Exception Returned ::: " + e.getMessage)
      metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
      Future.failed(new Exception(e.getMessage))
  }

  def submit(nino: String): Action[JsValue] = Action.async(parse.json) {
    implicit request => {
      logger.debug("RegistrationController submit request initiated")
      val pr = JsonValidator.validate(request.body, Constants.schemaPathRegistrationSubmission)
      if (pr.isSuccess) {
        logger.debug("DES Request Validated")
        logger.info("Acknowledgment Ref: " + request.body.\("acknowledgmentReference"))
        desConnector.
          submitRegistration(nino, request.body) map { httpResponse =>
            (Json.parse(httpResponse.body) \ "referenceNumber").asOpt[String].map { ihtRef =>
              logger.info("Parsed IHT Reference from response")
              metrics.incrementSuccessCounter(Api.SUB_REGISTRATION)
              val jsonValue = request.body
              auditService.sendEvent(
                Constants.AuditTypeIHTRegistrationSubmitted,
                jsonValue,
                Constants.AuditTypeIHTRegistrationSubmittedTransactionName
              )
              Ok(ihtRef)
            }.getOrElse {
                logger.info("Failure to parse IHTREF from response")
                InternalServerError("CAN NOT PARSE IHT REF FROM RESPONSE ")
            }
        }
      } else {
        Future.successful(processJsonValidationError(pr, request.body))
      }
    } recoverWith recoverOnSubmit
  }
}

