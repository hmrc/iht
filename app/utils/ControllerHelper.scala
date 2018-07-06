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

package utils

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.fge.jsonschema.core.report.{ProcessingMessage, ProcessingReport}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.play.http._
import metrics.Metrics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger
import models.enums._
import play.api.mvc.Results._
import uk.gov.hmrc.http.{BadRequestException, GatewayTimeoutException, NotFoundException, Upstream4xxResponse, Upstream5xxResponse}
import utils.exception.DESInternalServerError

/**
 *
 * Created by Vineet Tyagi on 16/09/15.
 *
 */
//scalastyle:off magic.number
object ControllerHelper {
    def metrics: Metrics = Metrics

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
        Logger.warn("Gateway Timeout Response Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(y)
        Future.failed(new GatewayTimeoutException(e.message + "des_gateway_timeout"))
      }
      case e: BadRequestException => {
        Logger.warn("BadRequest Response Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(y)
        Future.failed(new BadRequestException(e.message + "des_bad_request"))
      }
      case e: DESInternalServerError => {
        Logger.info(" Upstream5xxResponse Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(y)
        Future.failed(e)
      }
      case e: Upstream4xxResponse => {
        Logger.info(" Upstream4xxResponse Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(y)
        Future.failed(new Upstream4xxResponse(e.message, e.upstreamResponseCode, e.reportAs))
      }
      case e: Upstream5xxResponse => {
        Logger.info("Upstream5xxResponse Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(y)
        Future.failed(new Upstream5xxResponse(updateMessage(e.message,e.upstreamResponseCode), e.upstreamResponseCode, e.reportAs))
      }
      case e: NotFoundException => {
        Logger.info("Upstream4xxResponse Returned ::: " + e.getMessage)
        metrics.incrementFailedCounter(y)
        Future.failed(new Upstream4xxResponse(e.message + "des_not_found", notFoundExceptionCode, notFoundExceptionCode))
      }
      case e: Exception => {
        Logger.info("Exception Returned ::: " + e.getMessage)
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
    Logger.error("JSON validation against schema failed")
    val sb = new StringBuilder("Validator messages:-\n")
    val it = pr.iterator

    if (Some(it).isDefined) {
      while (it.hasNext) {
        val pm = it.next()
        Logger.error("Failure reasons  :::: " + jsonNodeByName(pm, "reports"))
        sb.append(pm.getMessage + "\n")
      }
    }

    Logger.error(pr.toString)

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
