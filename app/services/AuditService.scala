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
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.{HeaderFieldsExtractor, HttpAuditing}
import uk.gov.hmrc.play.audit.model.{DataEvent, MergedDataEvent, DataCall}
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

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

  def sendApplicationDataChangeEvent( detail: Map[String, String]) (implicit hc: HeaderCarrier, ec: ExecutionContext) = {
    sendEvent(AuditTypes.APPLICATION, detail)
  }

  def sendAssetDataChangeEvent(detail: Map[String, String]) (implicit hc: HeaderCarrier, ec: ExecutionContext) = {
    sendEvent(AuditTypes.ASSETS, detail)
  }

  def sendGiftDataChangeEvent(detail: Map[String, String]) (implicit hc: HeaderCarrier, ec: ExecutionContext) = {
    sendEvent(AuditTypes.GIFTS, detail)
  }

  def sendDebtDataChangeEvent(detail: Map[String, String]) (implicit hc: HeaderCarrier, ec: ExecutionContext) = {
    sendEvent(AuditTypes.DEBTS, detail)
  }

  def sendExemptionDataChangeEvent(detail: Map[String, String]) (implicit hc: HeaderCarrier, ec: ExecutionContext) = {
    sendEvent(AuditTypes.EXEMPTIONS, detail)
  }

  def sendTnrbDataChangeEvent(detail: Map[String, String]) (implicit hc: HeaderCarrier, ec: ExecutionContext) = {
    sendEvent(AuditTypes.TNRB, detail)
  }

  def sendSubmissionFailureEvent(detail: Map[String, String]) (implicit hc: HeaderCarrier, ec: ExecutionContext) = {
    sendEvent(AuditTypes.SUB_FAILURE, detail)
  }

  def auditRequestWithResponse(url: String, verb: String, body: Option[_], responseToAuditF: Future[HttpResponse])
                               (implicit hc: HeaderCarrier): Unit = {
    AuditingHook(url, verb, body,responseToAuditF)
  }

  def sendEvent(auditType: String, detail: Map[String, String])(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    auditConnector.sendEvent(ihtEvent(auditType, detail))

  //Creates iht event
  private def ihtEvent(auditType: String, detail: Map[String, String])(implicit hc: HeaderCarrier) =
    DataEvent(
      auditSource = "iht",
      auditType = auditType,
      tags = hc.headers.toMap,
      detail = detail)

  // Creates audit types
  object AuditTypes {
    val APPLICATION = "ApplicationDataChange"
    val ASSETS = "AssetDataChange"
    val GIFTS = "GiftDataChange"
    val DEBTS = "DebtDataChange"
    val EXEMPTIONS = "ExemptionsDataChange"
    val TNRB = "TnrbDataChange"
    val SUB_FAILURE = "OutboundCall"
  }

}
