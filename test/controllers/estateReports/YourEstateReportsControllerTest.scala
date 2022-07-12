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

package controllers.estateReports

import connectors.IhtConnector
import controllers.{Assets, ControllerComponentsHelper}
import metrics.MicroserviceMetrics
import models.enums._
import models.registration.RegistrationDetails
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsResult, Json}
import play.api.mvc.ControllerComponents
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.CommonHelper._
import utils.TestHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class YourEstateReportsControllerTest extends PlaySpec with MockitoSugar with BeforeAndAfterEach with ControllerComponentsHelper {

  val mockDesConnector: IhtConnector = mock[IhtConnector]
  val mockMetrics: MicroserviceMetrics = mock[MicroserviceMetrics]
  val mockControllerComponents: ControllerComponents = mock[ControllerComponents]
  val errorHttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, "")
  val badRequestHttpResponse = HttpResponse(BAD_REQUEST, "")
  val noListHttpResponse = HttpResponse(NO_CONTENT, "")
  val successHttpResponse = HttpResponse(OK, TestHelper.JsListCases)
  val successHttpResponseEmptyCaseList = HttpResponse(OK, TestHelper.JsEmptyListCases)
  val successHttpResponseNoNINO = HttpResponse(OK, TestHelper.JsListCasesNoNINO)

  val successHttpResponseForCaseDetails= HttpResponse(OK, TestHelper.JsSampleCaseDetailsString)
  val successHttpResponseForCaseDetailsWithPostCodeNull= HttpResponse(OK, TestHelper.JsSampleCaseDetailsString)

  implicit val headerCarrier = FakeHeaders()
  implicit val request = FakeRequest()
  implicit val hc = new HeaderCarrier

  override def beforeEach(): Unit = {
    reset(mockDesConnector)
    reset(mockMetrics)
    super.beforeEach()
  }

  def ihtHomeController: YourEstateReportsController = {
    class TestController extends BackendController(mockControllerComponents) with YourEstateReportsController {
      override val ihtConnector = mockDesConnector
      override val metrics: MicroserviceMetrics = mockMetrics
    }

    when(mockControllerComponents.actionBuilder)
      .thenReturn(testActionBuilder)

    new TestController
  }

  "Respond appropriately to a failure response" in {
    when(mockDesConnector.getCaseList(ArgumentMatchers.any())).thenReturn((Future(errorHttpResponse)))
    val result = ihtHomeController.listCases("")(request)
    status(result) should be(INTERNAL_SERVER_ERROR)
  }

  "Respond with no content if a case list is not available" in {
    when(mockDesConnector.getCaseList(ArgumentMatchers.any())).thenReturn((Future(noListHttpResponse)))
    val result = ihtHomeController.listCases("")(request)
    status(result) should be(NO_CONTENT)
  }

  "Respond with OK on successful return of a list" in {
    when(mockDesConnector.getCaseList(ArgumentMatchers.any())).thenReturn((Future(successHttpResponse)))
    val result = ihtHomeController.listCases("")(request)
    status(result) should be(OK)
    verify(mockMetrics, times(1)).incrementSuccessCounter(Api.GET_CASE_LIST)
  }

  "Respond with OK on successful return of a list when no NINO passed back" in {
    when(mockDesConnector.getCaseList(ArgumentMatchers.any())).thenReturn((Future(successHttpResponseNoNINO)))
    val result = ihtHomeController.listCases("")(request)
    status(result) should be(OK)
  }

  "Respond appropriately to a failure response while fetching Case Details" in {
    when(mockDesConnector.getCaseDetails(ArgumentMatchers.any(),ArgumentMatchers.any())).thenReturn((Future(errorHttpResponse)))
    val result = ihtHomeController.caseDetails("","")(request)
    status(result) should be(INTERNAL_SERVER_ERROR)
  }

  "Respond with no content if a Case Details is not available" in {
    when(mockDesConnector.getCaseDetails(ArgumentMatchers.any(),ArgumentMatchers.any())).thenReturn((Future(noListHttpResponse)))
    val result = ihtHomeController.caseDetails("","")(request)
    status(result) should be(NO_CONTENT)
  }

  "Respond with OK on successful return of Case Details" in {
    when(mockDesConnector.getCaseDetails(ArgumentMatchers.any(),ArgumentMatchers.any())).thenReturn(Future(successHttpResponseForCaseDetails))
    val result = ihtHomeController.caseDetails("","")(request)
    status(result) should be(OK)
    verify(mockMetrics, times(1)).incrementSuccessCounter(Api.GET_CASE_DETAILS)
  }

  "Respond with OK on successful return of Case Details when post code" +
    " is null/blank for foreign country address" in {
    when(mockDesConnector.getCaseDetails(ArgumentMatchers.any(),ArgumentMatchers.any())).thenReturn(Future(successHttpResponseForCaseDetailsWithPostCodeNull))
    val result = ihtHomeController.caseDetails("","")(request)
    status(result) should be(OK)
    verify(mockMetrics, times(1)).incrementSuccessCounter(Api.GET_CASE_DETAILS)
  }

  "Check the Custom Read function while Json.fromJson" in {

    val json = Json.parse(TestHelper.JsSampleCaseDetailsString)
    val jsResultAfterRead: JsResult[RegistrationDetails] = Json.fromJson[RegistrationDetails](json)(RegistrationDetails
      .registrationDetailsReads)

    val optionRD = jsResultAfterRead.fold[Option[RegistrationDetails]](_=>None, xx=>Some(xx))

    val rd = getOrException(optionRD)
    assert(getOrException(rd.ihtReference) =="1234567890ABCDE","Iht Reference is 1234567890ABCDE")
    assert(getOrException(rd.deceasedDetails).firstName=="Abc","Deceased First Name is Abc")
    assert(rd.returns.size>0 , "Returns details Seq size must be greater than zero")
    assert(rd.returns.head.submitterRole=="Lead Executor" , "Submitter role is Lead Executor")
    assert(getOrException(rd.returns.head.returnId) =="1234567890" , "ReturnId is 1234567890")
  }

  "Respond with exception to a failure response while fetching Case Details" in {
    when(mockDesConnector.getCaseDetails(ArgumentMatchers.any(),ArgumentMatchers.any())).thenReturn((Future(badRequestHttpResponse)))
    val result = ihtHomeController.caseDetails("","")(request)
    status(result) should be(INTERNAL_SERVER_ERROR)
  }

  "replies correctly when given an empty json response with a 200 return" in {
    when(mockDesConnector.getCaseList(ArgumentMatchers.any())).thenReturn(Future(successHttpResponseEmptyCaseList))
    val result = ihtHomeController.listCases("")(request)
    status(result) mustBe NO_CONTENT
  }

  "replies correctly when receiving a 404 from DES" in {
    when(mockDesConnector.getCaseList(ArgumentMatchers.any())).thenReturn(Future.failed(new NotFoundException("Cases not found")))
    val result = ihtHomeController.listCases("")(request)
    status(result) mustBe NO_CONTENT
  }
}
