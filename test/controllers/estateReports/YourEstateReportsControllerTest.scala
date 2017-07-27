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

package controllers.estateReports

import connectors.IhtConnector
import controllers.estateReports.YourEstateReportsController
import metrics.Metrics
import models.enums._
import models.registration.RegistrationDetails
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.{JsResult, Json}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.play.http.{HeaderCarrier, _}
import uk.gov.hmrc.play.test.UnitSpec
import utils.CommonHelper._
import utils.{FakeIhtApp, TestHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class YourEstateReportsControllerTest extends UnitSpec with FakeIhtApp with MockitoSugar {

  val mockDesConnector: IhtConnector = mock[IhtConnector]
  val errorHttpResponse = HttpResponse(INTERNAL_SERVER_ERROR,None,Map(),None)
  val badRequestHttpResponse = HttpResponse(BAD_REQUEST,None,Map(),None)
  val noListHttpResponse = HttpResponse(NO_CONTENT,None,Map(),None)
  val successHttpResponse = HttpResponse(OK,Some(Json.parse(TestHelper.JsListCases)),Map(),None)
  val successHttpResponseEmptyCaseList = HttpResponse(OK, Some(Json.parse(TestHelper.JsEmptyListCases)),Map(),None)
  val successHttpResponseNoNINO = HttpResponse(OK,Some(Json.parse(TestHelper.JsListCasesNoNINO)),Map(),None)

  val successHttpResponseForCaseDetails=HttpResponse(OK,Some(Json.parse(TestHelper.JsSampleCaseDetailsString)),Map())
  val successHttpResponseForCaseDetailsWithPostCodeNull=HttpResponse(OK,Some(Json.parse(TestHelper.JsSampleCaseDetailsString)),Map())

  implicit val headerCarrier = FakeHeaders()
  implicit val request = FakeRequest()
  implicit val hc = new HeaderCarrier

  def ihtHomeController = new YourEstateReportsController {
    override val ihtConnector = mockDesConnector
    override def metrics: Metrics = Metrics
  }

  "Respond appropriately to a failure response" in {
    when(mockDesConnector.getCaseList(any())).thenReturn((Future(errorHttpResponse)))
    val result = ihtHomeController.listCases("")(request)
    status(result) should be(INTERNAL_SERVER_ERROR)
  }

  "Respond with no content if a case list is not available" in {
    when(mockDesConnector.getCaseList(any())).thenReturn((Future(noListHttpResponse)))
    val result = ihtHomeController.listCases("")(request)
    status(result) should be(NO_CONTENT)
  }

  "Respond with OK on successful return of a list" in {
    when(mockDesConnector.getCaseList(any())).thenReturn((Future(successHttpResponse)))
    val result = ihtHomeController.listCases("")(request)
    status(result) should be(OK)
    assert(Metrics.successCounters(Api.GET_CASE_LIST).getCount>0, "Success counter for Get Case List Api is more than one")
  }

  "Respond with OK on successful return of a list when no NINO passed back" in {
    when(mockDesConnector.getCaseList(any())).thenReturn((Future(successHttpResponseNoNINO)))
    val result = ihtHomeController.listCases("")(request)
    status(result) should be(OK)
  }

  "Respond appropriately to a failure response while fetching Case Details" in {
    when(mockDesConnector.getCaseDetails(any(),any())).thenReturn((Future(errorHttpResponse)))
    val result = ihtHomeController.caseDetails("","")(request)
    status(result) should be(INTERNAL_SERVER_ERROR)
  }

  "Respond with no content if a Case Details is not available" in {
    when(mockDesConnector.getCaseDetails(any(),any())).thenReturn((Future(noListHttpResponse)))
    val result = ihtHomeController.caseDetails("","")(request)
    status(result) should be(NO_CONTENT)
  }

  "Respond with OK on successful return of Case Details" in {
    when(mockDesConnector.getCaseDetails(any(),any())).thenReturn(Future(successHttpResponseForCaseDetails))
    val result = ihtHomeController.caseDetails("","")(request)
    status(result) should be(OK)
    assert(Metrics.successCounters(Api.GET_CASE_DETAILS).getCount>0, "Success counter for Get Case Details Api is more than one")
  }

  "Respond with OK on successful return of Case Details when post code" +
    " is null/blank for foreign country address" in {
    when(mockDesConnector.getCaseDetails(any(),any())).thenReturn(Future(successHttpResponseForCaseDetailsWithPostCodeNull))
    val result = ihtHomeController.caseDetails("","")(request)
    status(result) should be(OK)
    assert(Metrics.successCounters(Api.GET_CASE_DETAILS).getCount>0, "Success counter for Get Case Details Api is more than one")
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
    when(mockDesConnector.getCaseDetails(any(),any())).thenReturn((Future(badRequestHttpResponse)))
    val result = ihtHomeController.caseDetails("","")(request)
    status(result) should be(INTERNAL_SERVER_ERROR)
  }

  "replies correctly when given an empty json response with a 200 return" in {
    when(mockDesConnector.getCaseList(any())).thenReturn((Future(successHttpResponseEmptyCaseList)))
    val result = ihtHomeController.listCases("")(request)

    val xxx:Boolean = Await.ready(result, Duration.Inf).value.map(_.isFailure).getOrElse(false)

    assert(xxx)
  }
}