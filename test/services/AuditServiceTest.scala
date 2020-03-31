/*
 * Copyright 2020 HM Revenue & Customs
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

import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsString
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}
import uk.gov.hmrc.play.test.UnitSpec
import utils._

import scala.concurrent.Future

class AuditServiceTest extends UnitSpec with FakeIhtApp with MockitoSugar with BeforeAndAfter {
  private val auditSource = "iht"
  private val auditType = "dummy audit type"
  private val transactionName = "dummy transaction name"
  private val path = "dummy path"
  private val pathKey = "path"

  implicit val headerCarrier = FakeHeaders()
  implicit val request = FakeRequest.apply("POST","",FakeHeaders(Seq(pathKey->path)),None)
  implicit val hc = new HeaderCarrier

  var mockedAuditConnector: AuditConnector = mock[AuditConnector]

  def auditService = new AuditService {
    override def auditConnector: AuditConnector = mockedAuditConnector
  }

  before {
    mockedAuditConnector = mock[AuditConnector]
  }

  "Audit service" must {
    "respond correctly to sendEvent with a Map" in {
      val detail = Map("abc"->"dummy value 1", "def" -> "dummy value 2")
      implicit val dataEventNapper = ArgumentCaptor.forClass(classOf[DataEvent])
      when(mockedAuditConnector.sendEvent(dataEventNapper.capture)(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(AuditResult.Success))
      val result = await(auditService.sendEvent(auditType, detail, transactionName))
      result shouldBe AuditResult.Success
      val actualDataEvent = dataEventNapper.getValue
      actualDataEvent.auditType shouldBe auditType
      actualDataEvent.auditSource shouldBe auditSource
      actualDataEvent.detail shouldBe detail
      actualDataEvent.tags.find( _._1 == pathKey) shouldBe Some( pathKey -> path )
    }

    "respond correctly to sendEvent with a JsValue" in {
      implicit val extendedDataEventNapper = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      when(mockedAuditConnector.sendExtendedEvent(extendedDataEventNapper.capture)(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(AuditResult.Success))
      val jsValue = JsString( "dummy value")
      val result = await(auditService.sendEvent(auditType, jsValue, transactionName))
      result shouldBe AuditResult.Success
      val actualDataEvent = extendedDataEventNapper.getValue
      actualDataEvent.auditType shouldBe auditType
      actualDataEvent.auditSource shouldBe auditSource
      actualDataEvent.detail shouldBe jsValue
      actualDataEvent.tags.find( _._1 == pathKey) shouldBe Some( pathKey -> path )
    }
  }
}
