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

package config

import akka.actor._
import com.google.inject.Provider
import connectors.securestorage._

import javax.inject.Inject
import play.api.Logger.logger
import play.api.http.DefaultHttpErrorHandler
import play.api.inject.ApplicationLifecycle
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}
import play.api.routing.Router
import play.api._
import reactivemongo.ReactiveMongoHelper
import reactivemongo.api.{FailoverStrategy, MongoConnection}
import utils.exception.DESInternalServerError
import views.html.helper.options

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.language.postfixOps

class ErrorHandler @Inject() (env: Environment,
                              config: Configuration,
                              sourceMapper: OptionalSourceMapper,
                              router: Provider[Router]) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {
  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    exception match {
      case DESInternalServerError(cause) =>
        logger.warn("500 or 503 response returned from DES", cause)
        Future.successful(BadGateway("500 or 503 response returned from DES"))
      case _ => super.onServerError(request, exception)
    }
  }
}

class ApplicationStart @Inject()(val lifecycle: ApplicationLifecycle,
                                 val conf: Configuration,
                                 val env: Environment,
                                 implicit val ec: ExecutionContext) extends ApplicationGlobal
trait ApplicationGlobal {
  val lifecycle: ApplicationLifecycle
  val conf: Configuration
  val env: Environment
  implicit val ec: ExecutionContext
  private lazy val driver = new reactivemongo.api.MongoDriver

  lifecycle.addStopHook { () => Future.successful(
      if (env.mode != Mode.Test) {
        driver.close(10 seconds)
      }
    )
  }

  def stringToPeriod(s : String) : org.joda.time.Period =
    org.joda.time.format.PeriodFormat.getDefault.parsePeriod(s)

  def stringToFDuration(s : String) : FiniteDuration =
    Duration.create(s).toMillis millis

  lazy val system = ActorSystem("iht")
  lazy val secureStorage: SecureStorage = {
    val platformKey = conf.getOptional[String]("securestorage.platformkey").getOrElse{throw new RuntimeException("securestorage.platformkey is not defined")}

    if (platformKey == "LOCALKEY") {logger.info("Secure storage key is LOCALKEY")} else {logger.info("Secure storage key is NOT LOCALKEY") }

    val dbConf = conf.getOptional[String]("securestorage.dbConfig").getOrElse(throw new RuntimeException("securestorage.dbConfig is not defined"))

    val helper: ReactiveMongoHelper = MongoConnection.parseURI(dbConf) match {
      case Success(MongoConnection.ParsedURI(hosts, options, _, Some(database), auth)) =>
        ReactiveMongoHelper(database, hosts.map(h => h._1 + ":" + h._2), auth.toList, Some(FailoverStrategy.default), options)
      case Success(MongoConnection.ParsedURI(_, _, _, None, _)) =>
        throw new Exception(s"Missing database name in mongodb.uri '$dbConf'")
      case Failure(e) => throw new Exception(s"Invalid mongodb.uri '$dbConf'", e)
    }

    TypedActor(system).typedActorOf(TypedProps(
      classOf[SecureStorage],
      new SecureStorageTypedActor(platformKey, helper.db)
    ), "securestorage")
  }

  val cleanerActor: Cancellable = {
    val maxDuration : org.joda.time.Period =
      conf.getOptional[String]("securestorage.maxDuration").
        map{
          stringToPeriod
        }.getOrElse{
        import com.github.nscala_time.time.Imports._
        13.months - Period.days(1)
      }

    val cleanerRunInterval : FiniteDuration =
      conf.getOptional[String]("securestorage.cleanerRunInterval").
        map{stringToFDuration}.getOrElse(1 hour)

    val cleaner = system.actorOf(Props{
      new CleanerActor(secureStorage, maxDuration)
    })

    system.scheduler.schedule(
      0 seconds, cleanerRunInterval, cleaner, "secureStoreCleaner"
    )
  }
}
