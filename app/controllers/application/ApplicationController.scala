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

package controllers.application

import config.ApplicationGlobal
import connectors.IhtConnector
import connectors.securestorage.SecureStorageController
import constants.{AssetDetails, Constants}
import javax.inject.Inject
import json.JsonValidator
import metrics.MicroserviceMetrics
import models.application.ProbateDetails._
import models.application.{ApplicationDetails, ClearanceRequest, ProbateDetails}
import models.des.IHTReturn
import models.des.realtimerisking.RiskInput
import models.enums.Api
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc._
import services.AuditService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class ApplicationControllerImpl @Inject()(val ihtConnector: IhtConnector,
                                          val metrics: MicroserviceMetrics,
                                          val registrationHelper: RegistrationHelper,
                                          val auditService: AuditService,
                                          val appGlobal: ApplicationGlobal,
                                          val cc: ControllerComponents) extends BackendController(cc) with ApplicationController {
  lazy val jsonValidator: JsonValidator.type = JsonValidator
}

trait ApplicationController extends BackendController with SecureStorageController with ControllerHelper {

  import com.github.fge.jsonschema.core.report.ProcessingReport

  def jsonValidator: JsonValidator
  def registrationHelper: RegistrationHelper
  def auditService: AuditService

  val ihtConnector: IhtConnector
  val metrics: MicroserviceMetrics

  /**
    * Save application details to secure storage using the IHT reference as the cache ID.
    */
  def save(nino: String, acknowledgementReference: String): Action[JsValue] = Action(parse.json) {
    implicit request => {
      request.body.validate[ApplicationDetails] match {
        case success: JsSuccess[ApplicationDetails] =>
          val applicationDetails = success.get
          val ihtRef = applicationDetails.ihtRef.getOrElse(throw new RuntimeException("No IHT Reference Present"))
          try {
            logger.info("Updating secure storage")
            // Explicit auditing check
            doExplicitAuditCheck(nino, acknowledgementReference, applicationDetails)
            secureStorage.update(ihtRef, acknowledgementReference, Json.toJson(applicationDetails))
            Ok
          } catch {
            case _: Exception =>
              logger.info("Failed to get a return from Secure Storage")
              InternalServerError
          }
        case _: JsError => BadRequest
      }
    }
  }

  /**
    * Retrieve application details previously saved to secure storage.
    *
    * @param ihtRef
    */
  def get(nino: String, ihtRef: String, acknowledgementReference: String): Action[AnyContent] = Action {
      logger.info("Fetching secure storage record. Acknowlegenent Reference " + acknowledgementReference)
      secureStorage.get(ihtRef, acknowledgementReference) match {
        case Some(jsValue) => logger.info("Secure storage returned record"); Ok(jsValue)
        case None => Ok(Json.toJson(new ApplicationDetails(status = Constants.AppStatusNotStarted,
          ihtRef = Some(ihtRef))
        ))
      }
  }

  /**
    * Get real-time risking message from DES.
    * At present the application details are not needed, only the registration details,
    * but in the future they will be, and the risking is applicable to applications,
    * which is why this is here rather than on the ihtHomeController.
    */
  def getRealtimeRiskingMessage(ihtAppReference: String, nino: String): Action[AnyContent] = Action.async {
      exceptionCheckForResponses({
        registrationHelper.getRegistrationDetails(nino, ihtAppReference) match {
          case None =>
            Future.successful(InternalServerError("No registration details found"))
          case Some(rd) =>
            val acknowledgmentReference = CommonHelper.generateAcknowledgeReference
            val ri = RiskInput.fromRegistrationDetails(rd, acknowledgmentReference)
            val desJson = Json.toJson(ri)

            logger.debug("DES json for real-time risking successfully generated:-\n" + Json.prettyPrint(desJson))
            val pr: ProcessingReport = jsonValidator.validate(desJson, Constants.schemaPathRealTimeRisking)
            if (pr.isSuccess) {
              logger.info("Request Validated")
              ihtConnector.submitRealtimeRisking(rd.ihtReference.getOrElse(""),
                ri.acknowledgementReference.getOrElse(""),
                desJson).map {
                httpResponse =>
                  httpResponse.status match {
                    case OK =>
                      metrics.incrementSuccessCounter(Api.SUB_REAL_TIME_RISKING)
                      processRealtimeRiskingResponse(httpResponse.body)
                    case _ => InternalServerError(httpResponse.status.toString)
                  }
              }
            } else {
              Future(processJsonValidationError(pr, desJson))
            }
        }
      }, Api.SUB_REAL_TIME_RISKING)
  }

  private def processRealtimeRiskingResponse(httpResponseBody: String): Result = {
    import models.des.realtimerisking.RiskResponse
    val jsValue = Json.parse(httpResponseBody)
    logger.info("Real-time risking response json:-\n" + Json.prettyPrint(jsValue))

    val riskResponse = jsValue.asOpt[RiskResponse]
    logger.debug("Created RiskResponse object:-\n" + riskResponse.toString)
    CommonHelper.getOrException(riskResponse).rulesFired match {
      case None =>
        logger.info("Real-time risking response: No rules fired.")
        NoContent
      case Some(rulesFired) =>
        if (rulesFired.isEmpty) {
          logger.info("Real-time risking response: No rules fired.")
          NoContent
        } else {
          logger.info("Real-time risking response: " + rulesFired.size + " rules fired.")

          val moneyRule = rulesFired.find(_.ruleID.getOrElse("")
            == AssetDetails.IHTReturnRuleIDBankAndBuildingSocietyAccounts)
          // For now any non-empty string indicates a matching rule is found:-
          moneyRule match {
            case Some(_) =>
              val ruleSupportingInfo = moneyRule.get.supportingInformation.fold("")(supInfo => supInfo)
              if (ruleSupportingInfo.nonEmpty) {
                logger.info("Rule Supporting Info :: " + ruleSupportingInfo)
              } else {
                logger.info("Rule Supporting Info is not available")
              }
              Ok(ruleSupportingInfo)
            case None =>
              NoContent
          }
        }
    }
  }

  def handleResponseFromDesSubmission(httpResponse: HttpResponse,
                                      ad: ApplicationDetails)(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] = {
    httpResponse.status match {
      case OK =>
        logger.info("Received response from DES")
        logger.debug("Response received ::: " + Json.prettyPrint(httpResponse.json))
        metrics.incrementSuccessCounter(Api.SUB_APPLICATION)
        val finalEstateValue = ad.estateValue
        val map = Map(Constants.AuditTypeValue -> finalEstateValue.toString(),
          Constants.AuditTypeIHTReference -> ad.ihtRef.getOrElse(""))
        auditService.sendEvent(Constants.AuditTypeFinalEstateValue,
          map,
          Constants.AuditTypeIHTEstateReportSubmittedTransactionName).flatMap { _ => val jsonValue = Json.toJson(ad)
          auditService.sendEvent(Constants.AuditTypeIHTEstateReportSubmitted,
            jsonValue,
            Constants.AuditTypeIHTEstateReportSubmittedTransactionName).map { _ =>
            processResponse(ad.ihtRef.get, httpResponse.body)
          }
        }
      case _ =>
        logger.info("No valid response from DES")
        Future.successful(InternalServerError(httpResponse.status.toString))
    }
  }


  def submit(ihtAppReference: String, nino: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      exceptionCheckForResponses({
        Try(request.body.as[ApplicationDetails]) match {
          case Success(ad) =>
            import org.joda.time.LocalDateTime
            // Create an app details object from json and convert it to json
            // using the current date and time as the declaration date,
            // generating an acknowledgement reference, then generate and
            // validate json in DES format.
            val declarationDate = new LocalDateTime
            val acknowledgmentReference = CommonHelper.generateAcknowledgeReference
            logger.info("About to convert to DES format")

            val registrationDetails = registrationHelper.getRegistrationDetails(nino, ihtAppReference)
            val odod = registrationDetails flatMap { _.deceasedDateOfDeath.map(_.dateOfDeath) }

            odod match {
              case None => Future.successful(InternalServerError("No registration details found"))
              case Some(dateOfDeath) =>
                val ir = IHTReturn.fromApplicationDetails(ad, declarationDate, acknowledgmentReference, dateOfDeath)
                val desJson = Json.toJson(ir)
                logger.debug("DES json successfully generated from application details object")
                val pr: ProcessingReport = jsonValidator.validate(desJson, Constants.schemaPathApplicationSubmission)
                if (pr.isSuccess) {
                  logger.info("DES Json successfully validated")
                  registrationDetails flatMap { _.applicantDetails } map { _.nino } match {
                    case Some(leadExecutorNino) if nino!=leadExecutorNino =>
                      logger.error(s"[ApplicationController][submit] Submission attempt is NOT the lead executor")
                      Future.successful(Forbidden("Submitter is not the lead executor"))
                    case _ =>
                      for {
                        httpResponse <- ihtConnector.submitApplication(nino, ad.ihtRef.getOrElse(""), desJson)
                        result <- handleResponseFromDesSubmission(httpResponse, ad)
                      } yield result
                  }
                } else {
                  Future(processJsonValidationError(pr, desJson))
                }
            }
          case Failure(ex) =>
            logger.info("Unable to extract front-end model object from JSON")
            Future.successful(InternalServerError("Unable to extract front-end model object from JSON:-" + ex.getMessage))
        }
      }, Api.SUB_APPLICATION)
  }

  def deleteRecord(nino: String, ihtReference: String): Action[AnyContent] = Action {
      logger.debug("Dropping record")
      secureStorage - ihtReference
      Ok
  }

  private def processResponse(ihtReference: String, httpResponseBody: String): Result = {
    (Json.parse(httpResponseBody) \ "returnId").asOpt[String] match {
      case Some(successResponse) =>
        // Removing the given Iht reference details from Secure storage
        secureStorage - ihtReference
        Ok("Success response received: " + successResponse)
      case None =>
        val jsonFailure = Json.parse(httpResponseBody) \ "failureResponse"
        jsonFailure.asOpt[String] match {
          case Some(_) =>
            (jsonFailure \ "reason").asOpt[String] match {
              case Some(reason) =>
                logger.info("Failure response received: " + reason)
                InternalServerError("Failure response received: " + reason)
              case None =>
                logger.info("Failure response received but no reason found.")
                InternalServerError("Failure response received but no reason found.")
            }
          case None =>
            logger.info("Neither success nor failure response received from DES.")
            InternalServerError("Neither success nor failure response received from DES.")
        }
    }
  }

  def requestClearance(nino: String, ihtReference: String): Action[AnyContent] = Action.async {
      exceptionCheckForResponses({
        val desJson = Json.toJson(ClearanceRequest(AcknowledgementRefGenerator.getUUID))
        val pr: ProcessingReport = jsonValidator.validate(desJson, Constants.schemaPathClearanceRequest)
        logger.info("Clearance Request Json for DES has been validated successfully")

        if (pr.isSuccess) {
          logger.debug("Request Clearance Validated")
          ihtConnector.requestClearance(nino, ihtReference, desJson).map {
            httpResponse =>
              httpResponse.status match {
                case OK =>
                  logger.info("Received response from DES (Clearance)")
                  metrics.incrementSuccessCounter(Api.SUB_REQUEST_CLEARANCE)
                  Ok("Clearance Granted")
                case ACCEPTED => InternalServerError("The request has been accepted but not processed immediately")
                case _ => InternalServerError(httpResponse.status.toString)
              }
          }
        } else {
          Future(processJsonValidationError(pr, desJson))
        }
      }, Api.SUB_REQUEST_CLEARANCE)
  }

  /*
   * Get the Probate details for given nino, ihtReference and ihtReturnId
   */
  def getProbateDetails(nino: String, ihtReference: String,
                        ihtReturnId: String): Action[AnyContent] = Action.async {
      exceptionCheckForResponses({
        ihtConnector.getProbateDetails(nino, ihtReference, ihtReturnId) map {
          httpResponse =>
            httpResponse.status match {
              case OK =>
                logger.info("Received response from DES")
                metrics.incrementSuccessCounter(Api.GET_PROBATE_DETAILS)

                val js: JsValue = Json.parse(httpResponse.body)
                val pr: ProcessingReport = jsonValidator.validate(js, Constants.schemaPathProbateDetails)

                if (pr.isSuccess) {
                  logger.info("DES Response Validated")
                  Ok(Json.toJson(processResponse(js))).as("text/json")
                } else {
                  processJsonValidationError(pr, js)
                }
              case NO_CONTENT =>
                logger.info("No contents in response from DES")
                NoContent
              case _ =>
                logger.info("No valid response from DES")
                InternalServerError

            }
        }
      }, Api.GET_PROBATE_DETAILS)
  }

  /**
    *
    * @param js
    * @return
    */
  private def processResponse(js: JsValue): ProbateDetails = {
    val probateDetails: ProbateDetails = Json.fromJson((js \ "probateTotals").get)(probateDetailsReads)
      .getOrElse(throw new RuntimeException("Probate Details response not parsed properly"))

    probateDetails
  }

  def getSubmittedApplicationDetails(nino: String, ihtReference: String, returnId: String): Action[AnyContent] = Action.async {
      ihtConnector.getSubmittedApplicationDetails(nino, ihtReference, returnId).map {
        httpResponse =>
          httpResponse.status match {
            case OK =>
              logger.debug("getSubmittedApplicationDetails response OK")
              metrics.incrementSuccessCounter(Api.GET_APPLICATION_DETAILS)
              val js: JsValue = Json.parse(httpResponse.body)
              val pr: ProcessingReport = jsonValidator.validate(js, Constants.schemaPathIhtReturn)
              if (pr.isSuccess) {
                logger.info("DES Response Validated")
                Ok(js)
              } else {
                processJsonValidationError(pr, js)
              }
            case _ => InternalServerError("Failed to return the requested application details")
          }
      }
  }

  /**
    * Does the explicit auditing if required
    *
    * @param appDetails
    */
  private def doExplicitAuditCheck(nino: String, acknowledgementReference: String, appDetails: ApplicationDetails)
                                  (implicit hc: HeaderCarrier, request: Request[_]): Seq[Future[AuditResult]] = {

    val securedStorageAppDetails: ApplicationDetails = getApplicationDetails(acknowledgementReference,
      CommonHelper.getOrException(appDetails.ihtRef))

    if (securedStorageAppDetails.status.equals(Constants.AppStatusInProgress)) {
      val appMap: Map[String, Map[String, String]] = AuditHelper.currencyFieldDifferences(securedStorageAppDetails, appDetails)
      appMap.keys.toSeq.map { current =>
        auditService.sendEvent(Constants.AuditTypeMonetaryValueChange,
          appMap(current),
          Constants.AuditTypeIHTEstateReportSaved).map { auditResult =>
          logger.debug(s"audit event sent for currency change: $appMap and audit result received of $auditResult")
          auditResult
        }
      }
    } else {
      Nil
    }
  }


  /**
    * Retrieves the application details object from secure storage
    *
    * @param acknowledgementReference
    * @param ihtRef
    * @return
    */
  private def getApplicationDetails(acknowledgementReference: String, ihtRef: String): ApplicationDetails = {

    Json.fromJson[ApplicationDetails](secureStorage.get(ihtRef, acknowledgementReference) match {
      case Some(jsValue) => logger.info("Secure storage returned record"); jsValue
      case None => Json.toJson(new ApplicationDetails(status = Constants.AppStatusNotStarted,
        ihtRef = Some(ihtRef)))
    }).get
  }
}
