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

package services

import config.wiring.MicroserviceAuditConnector
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.JsValue
import play.api.mvc.Request
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

/**
 *
 * Created by Vineet Tyagi on 13/11/15.
 *
 */
object AuditService extends AuditService{

}

trait AuditService extends HttpAuditing {
  override def auditConnector: AuditConnector = MicroserviceAuditConnector
  override def appName: String="iht"

  def sendSubmissionFailureEvent(detail: Map[String, String],
                                 transactionName: String) (implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_]) = {
    sendEvent(AuditTypes.SUB_FAILURE, detail, transactionName)
  }

  def auditRequestWithResponse(url: String, verb: String, body: Option[_], responseToAuditF: Future[HttpResponse])
                               (implicit hc: HeaderCarrier): Unit = {
    AuditingHook(url, verb, body,responseToAuditF)
  }

  def sendEvent(auditType: String,
                detail: Map[String, String],
                transactionName: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_]): Future[AuditResult] = {
    val event = DataEvent(
      auditSource = appName,
      auditType = auditType,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(transactionName, request.path),
      detail = detail,
      generatedAt = DateTime.now(DateTimeZone.UTC))
    println( "\n\n***sendEvent - DataEvent = " + event)
    auditConnector.sendEvent(event)
  }

  def sendEvent(auditType: String,
                detail: JsValue,
                transactionName: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_]): Future[AuditResult] = {
    val event = ExtendedDataEvent(
      auditSource = appName,
      auditType = auditType,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(transactionName, request.path),
      detail = detail,
      generatedAt = DateTime.now(DateTimeZone.UTC)
    )
    println( "\n\n***sendEvent - ExtendedDataEvent = " + event)
    auditConnector.sendExtendedEvent(event)
  }

  // Creates audit types
  object AuditTypes {
    val SUB_FAILURE = "OutboundCall"
  }
}
