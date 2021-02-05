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
import com.typesafe.config._
import org.scalatest._
import play.api.libs.json._
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class CleanerActorTest extends UnitSpec with WordSpecLike with BeforeAndAfter with MongoSpecSupport {

  val driver = new reactivemongo.api.MongoDriver
  val conn = driver.connection(Seq("localhost"))
  val db = await(conn.database("cleaneractortest"))

  val system = ActorSystem("TEST", ConfigFactory.parseString("""
    akka.stdout-loglevel = "OFF"
    akka.loglevel = "OFF"
  """))

  val timeout = akka.util.Timeout {
    import scala.concurrent.duration._
    5 seconds
  }

  val ss: SecureStorage =
    TypedActor(system).typedActorOf(TypedProps(classOf[SecureStorage],
      new SecureStorageTypedActor("PLATFORMKEY", mongo())), "ss")

  val cleaner = system.actorOf(Props{
    import com.github.nscala_time.time.Imports._
    new CleanerActor(ss, 0 seconds)
  })

  {
    import scala.concurrent.duration._
    system.scheduler.schedule(
      0 seconds, 100 millis, cleaner, "secureStoreCleaner"
    )
  }

  "A Cleaner Actor should remove an expired record automatically" in {
    import com.github.nscala_time.time.Imports._
    ss.update("somerecord","correctkey", JsArray(Seq(JsString("hiya"))))

    def now = org.joda.time.DateTime.now
    val timeout = now + (20 seconds)
    while(ss.get("somerecord","correctkey") != None && now.isBefore(timeout) ) {
      Thread.sleep(100)
    }
    assert(ss.get("somerecord","correctkey") == None)
  }

}
