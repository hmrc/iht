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

package controllers

import config.wiring.MicroserviceAuditConnector
import connectors.IHTConnector
import connectors.securestorage.SecureStorageController
import constants.{AssetDetails, Constants}
import json.JsonValidator
import metrics.Metrics
import models.ProbateDetails.probateDetailsReads
import models._
import models.des.IHTReturn
import models.des.realtimerisking.RiskInput
import models.enums._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.Action
import services.AuditService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.microservice.controller.BaseController
import utils.ControllerHelper._
import utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


object ApplicationController extends ApplicationController {
  lazy val desConnector = IHTConnector
  lazy val jsonValidator = JsonValidator
  lazy val registrationHelper = RegistrationHelper
  def metrics: Metrics = Metrics
}

trait ApplicationController extends BaseController with SecureStorageController{

  import com.github.fge.jsonschema.core.report.ProcessingReport

  def desConnector: IHTConnector
  def jsonValidator: JsonValidator
  def registrationHelper: RegistrationHelper
  def metrics: Metrics
  def auditConnector: AuditConnector = MicroserviceAuditConnector

  /**
   * Save application details to secure storage using the IHT reference as the cache ID.
   */
  def save(nino: String, acknowledgementReference: String) = Action.async(parse.json) {
    implicit request => {
      request.body.validate[ApplicationDetails] match {
        case success: JsSuccess[ApplicationDetails] => {
          val applicationDetails = success.get
          val ihtRef = applicationDetails.ihtRef.getOrElse(throw new RuntimeException("No IHT Reference Present"))
          try {
            Logger.info("Updating secure storage")
            // Explicit auditing check
            doExplicitAuditCheck(nino, acknowledgementReference, applicationDetails)
            secureStorage.update(ihtRef, acknowledgementReference, Json.toJson(applicationDetails))
            Future.successful(Ok)
          } catch {
            case e:Exception => {
              Logger.info("Failed to get a return from Secure Storage")
              Future.successful(InternalServerError)
            }
          }
        }
        case error: JsError => {
          Future.successful(BadRequest)
        }
      }
    }
  }
  /**
   * Retrieve application details previously saved to secure storage.
   * @param ihtRef
   */
  def get(nino: String, ihtRef:String, acknowledgementReference:String) = Action {
    implicit request => {
      Logger.info("Fetching secure storage record. Acknowlegenent Reference " + acknowledgementReference)
      secureStorage.get(ihtRef, acknowledgementReference) match {
        case Some(jsValue) => Logger.info("Secure storage returned record");Ok(jsValue)
        case None => Ok(Json.toJson(new ApplicationDetails(status = Constants.AppStatusNotStarted,
          ihtRef = Some(ihtRef))
        ))
      }
    }
  }

  /**
   * Get real-time risking message from DES.
   * At present the application details are not needed, only the registration details,
   * but in the future they will be, and the risking is applicable to applications,
   * which is why this is here rather than on the ihtHomeController.
   */
  def getRealtimeRiskingMessage(ihtAppReference: String, nino:String) = Action.async {
    implicit request => ControllerHelper.exceptionCheckForResponses ({
      registrationHelper.getRegistrationDetails(nino, ihtAppReference) match {
        case None => {
          Future.successful(InternalServerError("No registration details found"))
        }
        case Some(rd) => {
          val acknowledgmentReference = CommonHelper.generateAcknowledgeReference
          val ri = RiskInput.fromRegistrationDetails(rd, acknowledgmentReference)
          val desJson = Json.toJson(ri)

          Logger.debug("DES json for real-time risking successfully generated:-\n" + Json.prettyPrint(desJson))
          val pr:ProcessingReport = jsonValidator.validate(desJson, Constants.schemaPathRealTimeRisking)
          if (pr.isSuccess) {
            Logger.info("Request Validated")
            desConnector.submitRealtimeRisking(rd.ihtReference.getOrElse(""),
              ri.acknowledgementReference.getOrElse(""),
              desJson).map {
              httpResponse => httpResponse.status match {
                case OK => {
                  metrics.incrementSuccessCounter(Api.SUB_REAL_TIME_RISKING)
                  processRealtimeRiskingResponse(httpResponse.body)
                }
                case _ => InternalServerError(httpResponse.status.toString)
              }
            }
          } else { Future(processJsonValidationError(pr, desJson)) }
        }
      }
    },Api.SUB_REAL_TIME_RISKING)
  }

  private def processRealtimeRiskingResponse(httpResponseBody:String) = {
    import models.des.realtimerisking.RiskResponse
    val jsValue = Json.parse(httpResponseBody)
    Logger.info("Real-time risking response json:-\n" + Json.prettyPrint(jsValue))

    val riskResponse = jsValue.asOpt[RiskResponse]
    Logger.debug( "Created RiskResponse object:-\n" + riskResponse.toString())
    CommonHelper.getOrException(riskResponse).rulesFired match {
      case None => {
        Logger.info("Real-time risking response: No rules fired.")
        NoContent
      }
      case Some(rulesFired) => {
        if (rulesFired.size == 0 ) {
          Logger.info("Real-time risking response: No rules fired.")
          NoContent
        } else {
          Logger.info("Real-time risking response: " + rulesFired.size + " rules fired.")

          //val moneyRule = rulesFired.find( _.assetCode.getOrElse("")
          val moneyRule = rulesFired.find( _.ruleID.getOrElse("")
            == AssetDetails.IHTReturnRuleIDBankAndBuildingSocietyAccounts )
          // For now any non-empty string indicates a matching rule is found:-
          moneyRule match {
            case Some(_) => {

              val ruleSupportingInfo = moneyRule.get.supportingInformation.fold("")(supInfo=>supInfo)

              if (ruleSupportingInfo.nonEmpty){
                Logger.info("Rule Supporting Info :: "+ruleSupportingInfo)
              } else {
                Logger.info("Rule Supporting Info is not available")
              }

              Ok(ruleSupportingInfo)
            }
            case None => {
              NoContent
            }
          }
        }
      }
    }
  }

  def submit(ihtAppReference: String, nino:String) = Action.async(parse.json) {
    implicit request => ControllerHelper.exceptionCheckForResponses ({
      Try(request.body.as[ApplicationDetails]) match {
        case Success(ad) => {
          import org.joda.time.LocalDateTime
          // Create an app details object from json and convert it to json
          // using the current date and time as the declaration date,
          // generating an acknowledgement reference, then generate and
          // validate json in DES format.
          val declarationDate = new LocalDateTime
          val acknowledgmentReference = CommonHelper.generateAcknowledgeReference

          Logger.info("About to convert to DES format")

          val odod = registrationHelper.getRegistrationDetails(nino, ihtAppReference) match {
            case None => {
              None
            }
            case Some(rd) => {
              rd.deceasedDateOfDeath.map(_.dateOfDeath)
            }
          }

          odod match {
            case None => Future.successful(InternalServerError("No registration details found"))
            case Some(dateOfDeath) => {
              val ir = IHTReturn.fromApplicationDetails(ad, declarationDate, acknowledgmentReference, dateOfDeath)
              val desJson = Json.toJson(ir)
              Logger.debug("DES json successfully generated from application details object")
              val pr:ProcessingReport = jsonValidator.validate(desJson, Constants.schemaPathApplicationSubmission)
              if (pr.isSuccess) {
                Logger.info("DES Json successfully validated")
                desConnector.submitApplication(nino, ad.ihtRef.getOrElse(""), desJson).map {
                  httpResponse => httpResponse.status match {
                    case OK => {
                      Logger.info("Received response from DES")
                      Logger.debug("Response received ::: " + Json.prettyPrint(httpResponse.json))
                      metrics.incrementSuccessCounter(Api.SUB_APPLICATION)
                      processResponse(ad.ihtRef.get, httpResponse.body)
                    }
                    case _ => {
                      Logger.info("No valid response from DES")
                      InternalServerError(httpResponse.status.toString)
                    }
                  }
                }
              } else {
                Future(processJsonValidationError(pr, desJson))
              }
            }
          }
        }
        case Failure(ex) => {
          Logger.info("Unable to extract front-end model object from JSON")
          Future.successful(InternalServerError("Unable to extract front-end model object from JSON:-" + ex.getMessage ))
        }
      }
    },Api.SUB_APPLICATION)
  }

  def deleteRecord(nino: String, ihtReference: String) = Action {
    implicit request => {
      Logger.debug("Dropping record")
      secureStorage - ihtReference
      Ok
    }
  }

  private def processResponse(ihtReference:String,httpResponseBody:String) = {
    (Json.parse(httpResponseBody) \ "returnId").asOpt[String] match {
      case Some(successResponse) => {
        // Removing the given Iht reference details from Secure storage
        secureStorage - ihtReference
        Ok("Success response received: " + successResponse)
      }
      case None => {
        val jsonFailure = Json.parse(httpResponseBody) \ "failureResponse"
        jsonFailure.asOpt[String] match {
          case Some(x) => {
            (jsonFailure \ "reason").asOpt[String] match {
              case Some(reason) => {
                Logger.info("Failure response received: " + reason)
                InternalServerError("Failure response received: " + reason)
              }
              case None => {
                Logger.info("Failure response received but no reason found.")

                InternalServerError("Failure response received but no reason found.")
              }
            }
          }
          case None => {
            Logger.info("Neither success nor failure response received from DES.")

            InternalServerError("Neither success nor failure response received from DES.")
          }
        }
      }
    }
  }

  def requestClearance(nino:String, ihtReference:String) = Action.async {
    implicit request => ControllerHelper.exceptionCheckForResponses ({
      val desJson = Json.toJson(ClearanceRequest(AcknowledgeRefGenerator.getUUID))
      val pr:ProcessingReport = jsonValidator.validate(desJson, Constants.schemaPathClearanceRequest)
      Logger.info("Clearance Request Json for DES has been validated successfully")

      if (pr.isSuccess) {
          Logger.debug("Request Clearance Validated")
          desConnector.requestClearance(nino, ihtReference, desJson).map {
          httpResponse => httpResponse.status match {
            case OK =>  {
              Logger.info("Received response from DES (Clearance)")
              metrics.incrementSuccessCounter(Api.SUB_REQUEST_CLEARANCE)
              Ok("Clearance Granted")
            }
            case ACCEPTED => InternalServerError("The request has been accepted but not processed immediately")
            case _ => InternalServerError(httpResponse.status.toString)
          }
        }
      } else {
        Future(processJsonValidationError(pr, desJson))
      }
    },Api.SUB_REQUEST_CLEARANCE)
  }

  /*
   * Get the Probate details for given nino, ihtReference and ihtReturnId
   */
  def getProbateDetails(nino:String, ihtReference:String, ihtReturnId: String) = Action.async {
    implicit request => ControllerHelper.exceptionCheckForResponses ({
      desConnector.getProbateDetails(nino, ihtReference, ihtReturnId) map {
        httpResponse => httpResponse.status match {
          case OK => {
            Logger.info("Received response from DES")
            metrics.incrementSuccessCounter(Api.GET_PROBATE_DETAILS)
            val js: JsValue = Json.parse(httpResponse.body)

            val pr:ProcessingReport = jsonValidator.validate(js, Constants.schemaPathProbateDetails)
            if(pr.isSuccess) {
              Logger.info("DES Response Validated")
              Ok(Json.toJson(processResponse(js))).as("text/json")
            } else {
              processJsonValidationError(pr, js)
            }
          }
          case NO_CONTENT => {
            Logger.info("No contents in response from DES")
            NoContent
          }
          case (_) => {
            Logger.info("No valid response from DES")
            InternalServerError
          }

        }
      }
    },Api.GET_PROBATE_DETAILS)
  }

  /**
   *
   * @param js
   * @return
   */
  private def processResponse(js : JsValue) : ProbateDetails = {
    val probateDetails: ProbateDetails = Json.fromJson((js \ "probateTotals").get)(probateDetailsReads)
      .getOrElse(throw new RuntimeException("Probate Details response not parsed properly"))

    probateDetails
  }

  def getSubmittedApplicationDetails(nino:String, ihtReference:String, returnId:String) = Action.async {
    implicit request => desConnector.getSubmittedApplicationDetails(nino, ihtReference, returnId).map {
      httpResponse => httpResponse.status match {
        case OK => {
          Logger.debug("getSubmittedApplicationDetails response OK")
          metrics.incrementSuccessCounter(Api.GET_APPLICATION_DETAILS)
          val js:JsValue = Json.parse(httpResponse.body)
          val pr:ProcessingReport = jsonValidator.validate(js, Constants.schemaPathIhtReturn)
          if (pr.isSuccess) {
            Logger.info("DES Response Validated")
            Ok(js)
          } else {
            processJsonValidationError(pr, js)
          }
        }
        case ( _ ) => {
          InternalServerError("Failed to return the requested application details")
        }
      }
    }
  }

  /**
   * Does the explicit auditing if requires
   * @param appDetails
   */
  private def doExplicitAuditCheck(nino: String, acknowledgementReference: String, appDetails: ApplicationDetails)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext) ={

    val securedStorageAppDetails: ApplicationDetails = getApplicationDetails(acknowledgementReference,
      CommonHelper.getOrException(appDetails.ihtRef))

    if (securedStorageAppDetails.status.equals(Constants.AppStatusInProgress)) {
      val appMap: Map[String, Map[String, String]] = ModelHelper.currencyFieldDifferences(securedStorageAppDetails, appDetails)
      if(appMap.nonEmpty) appMap.keys foreach { key => AuditService.sendEvent(key, appMap(key)) }
    }
  }

  /**
   * Retrieves the application details object from secure storage
   * @param acknowledgementReference
   * @param ihtRef
   * @return
   */
  private def getApplicationDetails( acknowledgementReference: String , ihtRef: String): ApplicationDetails = {

    Json.fromJson[ApplicationDetails](secureStorage.get(ihtRef, acknowledgementReference) match {
      case Some(jsValue) => Logger.info("Secure storage returned record");jsValue
      case None => Json.toJson(new ApplicationDetails(status = Constants.AppStatusNotStarted,
        ihtRef = Some(ihtRef)))
    }).get
  }
}
