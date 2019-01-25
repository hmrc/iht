/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.nscala_time.time.Imports._
import akka.actor.Actor
import org.joda.time.Period

class CleanerActor(
  val securestorage : SecureStorage,
  expiryDuration : Period
  ) extends Actor {

  import CleanerActor._

  var duration : Period = expiryDuration
  def receive = {
    case UpdateDuration(x) => duration = x
    case _ => securestorage.clean(DateTime.now - duration)
  }
}

object CleanerActor {
  case class UpdateDuration(newPeriod : org.joda.time.Period)
}
