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

package connectors.securestorage

import akka.actor._
import akka.util._
import config.ApplicationGlobal
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.language.postfixOps
import scala.concurrent.duration._

/**
  * A mixin to supply the secure storage interface to a controller
  */
trait SecureStorageController {
  self : BackendController =>

  val appGlobal: ApplicationGlobal

  lazy val secureStorage : SecureStorage = {
    implicit val timeout = Timeout(1 second)
    val f = appGlobal.system.actorSelection("user/securestorage").resolveOne
    val untypedActor = scala.concurrent.Await.result(f, 1 second)
    TypedActor(appGlobal.system).typedActorOf(
      TypedProps[SecureStorage], untypedActor
    )
  }
}
