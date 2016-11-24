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

package controllers

import connectors.IHTConnector
import constants.Constants
import json.JsonValidator
import metrics.Metrics
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Action
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.microservice.controller.BaseController
import utils.ControllerHelper._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import models.enums._

/**
 * Created by yasar on 2/5/15.
 */
object RegistrationController extends RegistrationController {
  val desConnector = IHTConnector
  override def metrics: Metrics = Metrics
}


trait RegistrationController extends BaseController {
  val desConnector: IHTConnector
  def metrics: Metrics = Metrics

  def submit(nino:String) = Action.async(parse.json) {
    implicit request => {
      Logger.debug("RegistrationController submit request initiated")
      val pr = JsonValidator.validate(request.body, Constants.schemaPathRegistrationSubmission)
      if (pr.isSuccess) {
        Logger.debug("DES Request Validated")
        Logger.info("Acknowledgment Ref: " + request.body.\("acknowledgmentReference"))
        desConnector.submitRegistration(nino, request.body).map {
          httpResponse => {
            Logger.debug("http response status code " + httpResponse.status)
            Logger.debug("Response " + Json.prettyPrint(httpResponse.json))
              ((Json.parse(httpResponse.body) \ "referenceNumber")).asOpt[String] match {
                case Some(ihtRef) => {
                  Logger.info("Parsed IHT Reference from response")
                  metrics.incrementSuccessCounter(Api.SUB_REGISTRATION)
                  Ok(ihtRef)
                }
                case None => {
                  Logger.info("Failure to parse IHTREF from response")
                  InternalServerError("CAN NOT PARSE IHT REF FROM RESPONSE ")
                }
            }
          }
        }
      } else {
        Future(processJsonValidationError(pr, request.body))
      }
    }.recoverWith {
      case Upstream4xxResponse(message, CONFLICT, _, _) => {

        metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
        Future.failed(new ConflictException(message))
      }
      case e:GatewayTimeoutException =>{

        Logger.warn("Gateway Timeout Response Returned ::: " + e.getMessage)
        Future.failed(new GatewayTimeoutException(e.message))
      }
      case e: BadRequestException => {

        Logger.warn("BadRequest Response Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
        Future.failed(new BadRequestException(e.message))
      }
      case e: Upstream4xxResponse => {

        Logger.info(" Upstream4xxResponse Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
        Future.failed(new Upstream4xxResponse(e.message, e.upstreamResponseCode, e.reportAs))
      }
      case e: Upstream5xxResponse => {

        Logger.info("Upstream5xxResponse Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
        Future.failed(new Upstream5xxResponse(e.message, e.upstreamResponseCode, e.reportAs))
      }
      case e: NotFoundException => {
        Logger.info("Upstream4xxResponse Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
        Future.failed(new Upstream4xxResponse(e.message, NOT_FOUND, NOT_FOUND))
      }
      case e: Exception => {
        Logger.info("Exception Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(Api.SUB_REGISTRATION)
        Future.failed(new Exception(e.getMessage))
      }
    }
  }
}
