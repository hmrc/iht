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

package config

import akka.actor._
import com.typesafe.config.Config
import connectors.{ApplicationAuditConnector, ApplicationAuthConnector}
import connectors.securestorage._
import net.ceedubs.ficus.Ficus.{configValueReader, toFicusConfig}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{RequestHeader, Result}
import play.api.{Application, Configuration, Logger, Play}
import play.libs.Akka
import uk.gov.hmrc.play.auth.controllers.AuthParamsControllerConfig
import uk.gov.hmrc.play.auth.microservice.filters.AuthorisationFilter
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.microservice.bootstrap.DefaultMicroserviceGlobal
import uk.gov.hmrc.play.microservice.filters.{AuditFilter, LoggingFilter, MicroserviceFilterSupport}
import utils.exception.DESInternalServerError
import play.api.mvc.Results._
import reactivemongo.api.MongoConnectionOptions

import scala.concurrent.Future
import scala.concurrent.duration._

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object AuthParamsControllerConfiguration extends AuthParamsControllerConfig {
  lazy val controllerConfigs = ControllerConfiguration.controllerConfigs
}

object MicroserviceAuditFilter extends AuditFilter with AppName with MicroserviceFilterSupport{
  override val auditConnector = ApplicationAuditConnector
  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

object MicroserviceLoggingFilter extends LoggingFilter with MicroserviceFilterSupport{
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object MicroserviceAuthFilter extends AuthorisationFilter with MicroserviceFilterSupport{
  override lazy val authParamsConfig = AuthParamsControllerConfiguration
  override lazy val authConnector = ApplicationAuthConnector
  override def controllerNeedsAuth(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsAuth
}

object ApplicationGlobal extends DefaultMicroserviceGlobal with RunMode {
  override val auditConnector = ApplicationAuditConnector

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig("microservice.metrics")

  override val loggingFilter = MicroserviceLoggingFilter

  override val microserviceAuditFilter = MicroserviceAuditFilter

  override val authFilter = Some(MicroserviceAuthFilter)
  private val driver = new reactivemongo.api.MongoDriver

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    ex match {
      case DESInternalServerError(cause) =>
        Logger.warn("500 response returned from DES", cause)
        Future.successful(BadGateway("500 response returned from DES"))
      case _ => super.onError(request, ex)
    }
    
  }

  override def onStart(app: Application) {
    super.onStart(app)
    val conf = app.configuration

    val secureStorage : SecureStorage = {
      val platformKey = conf.getString("securestorage.platformkey").getOrElse {
        throw new RuntimeException("securestorage.platformkey is not defined")
      }
      if (platformKey == "LOCALKEY") {
        Logger.info("Secure storage key is LOCALKEY")
      }else {
        Logger.info("Secure storage key is NOT LOCALKEY")
      }

      val host = conf.getString("securestorage.host").getOrElse("localhost")
      val dbName = conf.getString("securestorage.dbname").getOrElse("securestorage")
      val conn = driver.connection(host.split(","), MongoConnectionOptions(sslEnabled = true))
      val db = conn(dbName)

      TypedActor(Akka.system).typedActorOf(TypedProps(
        classOf[SecureStorage],
        new SecureStorageTypedActor(platformKey, db)
      ), "securestorage")
    }

    val cleanerActor: Cancellable = {

      val maxDuration : org.joda.time.Period =
        conf.getString("securestorage.maxDuration").
          map{
          stringToPeriod
        }.getOrElse{
          import com.github.nscala_time.time.Imports._
          13.months - Period.days(1)
        }

      val cleanerRunInterval : FiniteDuration =
        conf.getString("securestorage.cleanerRunInterval").
          map{stringToFDuration}.getOrElse(1 hour)

      val cleaner = Akka.system.actorOf(Props{
        new CleanerActor(secureStorage, maxDuration)
      })

      Akka.system.scheduler.schedule(
        0 seconds, cleanerRunInterval, cleaner, "secureStoreCleaner"
      )
    }
  }

  def stringToPeriod(s : String) : org.joda.time.Period =
    org.joda.time.format.PeriodFormat.getDefault.parsePeriod(s)

  def stringToFDuration(s : String) : FiniteDuration =
    Duration.create(s).toMillis millis

  override def onStop(app: Application) {
    super.onStop(app)
    /*
     * We need to terminate the mongo store used by secure storage, but
     * if done during testing this will close down the akka actor and
     * prevent subsequent tests from running.
     */
    if (env.toUpperCase != "TEST") {
      driver.close(10 seconds)
    }

  }
}
