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

package services

import javax.inject.Inject
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.Request
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}
import java.time.{Clock, Instant}

import play.api.Logger.logger
import uk.gov.hmrc.http.hooks.HookData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  *
  * Created by Vineet Tyagi on 13/11/15.
  *
  */
class AuditServiceImpl @Inject()(val auditConnect: AuditConnector) extends AuditService {
  override def auditConnector: AuditConnector = auditConnect
}

trait AuditService extends HttpAuditing {
  override def appName: String="iht"

  private val pathKey = "path"

  def sendSubmissionFailureEvent(detail: Map[String, String],
                                 transactionName: String) (implicit hc: HeaderCarrier, request: Request[_]) = {
    sendEvent(AuditTypes.SUB_FAILURE, detail, transactionName)
  }

  def auditRequestWithResponse(url: String, verb: String, body: String, responseToAuditF: Future[HttpResponse])
                              (implicit hc: HeaderCarrier): Unit = {
    AuditingHook(url, verb, Option(HookData.FromString(body)), responseToAuditF)
  }

  private def tags(transactionName: String)(implicit hc: HeaderCarrier, request: Request[_]) = {
    AuditExtensions.auditHeaderCarrier(hc).toAuditTags(transactionName, requestPath)
  }

  def requestPath(implicit request: Request[_]) = {
    request.headers.get(pathKey) match {
      case Some(path) => path
      case None => logger.warn(s"No path header supplied from IHT frontend on request to backend endpoint ${request.path}")
        "<NO REQUEST PATH SUPPLIED>"
    }
  }

  def sendEvent(auditType: String,
                detail: Map[String, String],
                transactionName: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[AuditResult] = {
    val event = DataEvent(
      auditSource = appName,
      auditType = auditType,
      tags = tags(transactionName),
      detail = detail,
      generatedAt = Instant.now())
    auditConnector.sendEvent(event)
  }

  def sendEvent(auditType: String,
                detail: JsValue,
                transactionName: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[AuditResult] = {

    val event = ExtendedDataEvent(
      auditSource = appName,
      auditType = auditType,
      tags = tags(transactionName),
      detail = detail,
      generatedAt = Instant.now())
    auditConnector.sendExtendedEvent(event)
  }

  // Creates audit types
  object AuditTypes {
    val SUB_FAILURE = "OutboundCall"
  }
}
