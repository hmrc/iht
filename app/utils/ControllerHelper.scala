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

package utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonschema.core.report.{ProcessingMessage, ProcessingReport}
import metrics.MicroserviceMetrics
import models.enums._
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.http._
import utils.exception.DESInternalServerError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 *
 * Created by Vineet Tyagi on 16/09/15.
 *
 */
//scalastyle:off magic.number
trait ControllerHelper extends Logging {
    def metrics: MicroserviceMetrics

    val desErrorCode502 = "des_error_code_502"
    val desErrorCode503 = "des_error_code_503"
    val desErrorCode504 = "des_error_code_504"
    val notFoundExceptionCode = 404
  /**
   * Checks the relevant response exceptions for the code to be executed
   */
  def exceptionCheckForResponses[A,Api](x : Future[A], y: Api.Api) : Future[A] =
    x.recoverWith {
      case e:GatewayTimeoutException =>{
        logger.warn("Gateway Timeout Response Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(y)
        Future.failed(new GatewayTimeoutException(e.message + "des_gateway_timeout"))
      }
      case e: BadRequestException => {
        logger.warn("BadRequest Response Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(y)
        Future.failed(new BadRequestException(e.message + "des_bad_request"))
      }
      case e: DESInternalServerError => {
        logger.info(" Upstream5xxResponse Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(y)
        Future.failed(e)
      }
      case e: UpstreamErrorResponse => {
        logger.info("UpstreamErrorResponse Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(y)
        Future.failed(UpstreamErrorResponse.apply(updateMessage(e.message,e.statusCode), e.statusCode, e.reportAs))
      }
      case e: NotFoundException => {
        logger.info("Upstream4xxResponse Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(y)
        Future.failed(UpstreamErrorResponse.apply(e.message + "des_not_found", notFoundExceptionCode, notFoundExceptionCode))
      }
      case e: Exception => {
        logger.info("Exception Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(y)
        Future.failed(new Exception(e.getMessage))
      }
    }

  /**
   *
   * Add the additional string to identify response code in front end
   *
   * @param msg
   * @param responseCode
   * @return
   */
  private def updateMessage(msg: String , responseCode: Int)= {
      responseCode match {
        case 502 => msg + desErrorCode502
        case 503 => msg + desErrorCode503
        case 504 => msg + desErrorCode504
        case _ => msg
      }
    }

  /**
   *
   * @param pr
   * @param desJson
   */
  def processJsonValidationError(pr: ProcessingReport, desJson: JsValue): Result = {
    logger.error("JSON validation against schema failed")
    val sb = new StringBuilder("Validator messages:-\n")
    val it = pr.iterator

    if (Some(it).isDefined) {
      while (it.hasNext) {
        val pm = it.next()
        logger.error("Failure reasons  :::: " + jsonNodeByName(pm, "reports"))
        sb.append(pm.getMessage + "\n")
      }
    }

    sb.append("JSON:-\n")
    sb.append(Json.prettyPrint(desJson))
    InternalServerError("JSON validation against schema failed")
  }

  /**
   * Get the json value as string for the given node name for ProcessingMessage
   * @param processingMessage
   * @param nodeName
   * @return
   */
  def jsonNodeByName(processingMessage: ProcessingMessage, nodeName: String): String = {
    val node = processingMessage.asJson().get(nodeName)
    val mapper = new ObjectMapper()
    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
  }
}
