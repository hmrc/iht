/*
 * Copyright 2016 HM Revenue & Customs
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
import connectors.securestorage._
import net.ceedubs.ficus.Ficus.configValueReader
import net.ceedubs.ficus.Ficus.toFicusConfig
import play.api.Application
import play.api.Configuration
import play.api.Play
import play.libs.Akka
import uk.gov.hmrc.play.audit.filters.AuditFilter
import uk.gov.hmrc.play.auth.controllers.AuthParamsControllerConfig
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.config.ControllerConfig
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.http.logging.filters.LoggingFilter
import uk.gov.hmrc.play.microservice.bootstrap.DefaultMicroserviceGlobal
import uk.gov.hmrc.play.auth.microservice.filters.AuthorisationFilter
import connectors.ApplicationAuditConnector
import connectors.ApplicationAuthConnector
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object AuthParamsControllerConfiguration extends AuthParamsControllerConfig {
  lazy val controllerConfigs = ControllerConfiguration.controllerConfigs
}

object MicroserviceAuditFilter extends AuditFilter with AppName {
  override val auditConnector = ApplicationAuditConnector
  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

object MicroserviceLoggingFilter extends LoggingFilter {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object MicroserviceAuthFilter extends AuthorisationFilter {
  override lazy val authParamsConfig = AuthParamsControllerConfiguration
  override lazy val authConnector = ApplicationAuthConnector
  override def controllerNeedsAuth(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsAuth
}

object ApplicationGlobal extends DefaultMicroserviceGlobal with RunMode {
  override val auditConnector = ApplicationAuditConnector

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"$env.microservice.metrics")

  override val loggingFilter = MicroserviceLoggingFilter

  override val microserviceAuditFilter = MicroserviceAuditFilter

  override val authFilter = Some(MicroserviceAuthFilter)
  private val driver = new reactivemongo.api.MongoDriver

  override def onStart(app: Application) {
    super.onStart(app)

    val conf = app.configuration

    val secureStorage : SecureStorage = {
      val platformKey = conf.getString(s"$env.securestorage.platformkey").getOrElse {
        throw new RuntimeException(s"$env.securestorage.platformkey is not defined")
      }
      val host = conf.getString(s"$env.securestorage.host").getOrElse("localhost")
      val dbName = conf.getString(s"$env.securestorage.dbname").getOrElse("securestorage")

      val conn = driver.connection(host.split(","))
      val db = conn(dbName)

      TypedActor(Akka.system).typedActorOf(TypedProps(
        classOf[SecureStorage],
        new SecureStorageTypedActor(platformKey, db)
      ), "securestorage")
    }

    val cleanerActor = {

      val maxDuration : org.joda.time.Period =
        conf.getString(s"$env.securestorage.maxDuration").
          map{
          stringToPeriod
        }.getOrElse{
          import com.github.nscala_time.time.Imports._
          13.months - Period.days(1)
        }

      val cleanerRunInterval : FiniteDuration =
        conf.getString(s"$env.securestorage.cleanerRunInterval").
          map{stringToFDuration}.getOrElse(1 hour)

      val cleaner = Akka.system.actorOf(Props{
        import com.github.nscala_time.time.Imports._
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
