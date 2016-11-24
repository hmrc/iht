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

package connectors.securestorage

import org.scalatest._

import play.api.libs.json._
import akka.pattern._
import akka.actor._
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalacheck._
import org.scalacheck.Shrink._
import com.typesafe.config._
import scala.concurrent.duration._

trait SecureStorageBehaviours extends org.scalatest.prop.Checkers {
  this: FlatSpec =>

  import org.scalacheck.Prop

  def genFields: Gen[(String, JsValue)] = {
    Gen.zip(Gen.identifier, Gen.oneOf(
      genJsPrimitive,
      genJsArray
  ))}

  def genJsObject: Gen[JsObject] = {
    for {
      fields <- Gen.listOfN(3, genFields)
    } yield JsObject(fields)
  }

  def genJsPrimitive: Gen[JsValue] = Gen.oneOf(
      arbJsBoolean.arbitrary,
      arbJsNumber.arbitrary,
      arbJsString.arbitrary,
      Gen.const(JsNull)
    )

  def genJsValue : Gen[JsValue] = Gen.oneOf(
      genJsPrimitive,
      genJsArray,
      genJsObject
  )

  def genJsArray: Gen[JsArray] =
    Gen.listOfN(4, genJsPrimitive) map { JsArray(_) }

   implicit def arbJsNumber(implicit arbBigDec: Arbitrary[Int]): Arbitrary[JsNumber] = Arbitrary {
     arbBigDec.arbitrary.map(x => JsNumber(x))
   }
  implicit def arbJsValue: Arbitrary[JsValue] = Arbitrary(genJsValue)
  implicit def arbJsObject: Arbitrary[JsObject] = Arbitrary(genJsObject)
  implicit def arbJsArray: Arbitrary[JsArray] = Arbitrary(genJsArray)

  implicit def arbJsBoolean: Arbitrary[JsBoolean] = Arbitrary {
    Gen.oneOf(true, false) map JsBoolean
  }

  implicit def arbJsString(implicit arbString: Arbitrary[String]): Arbitrary[JsString] = Arbitrary {
    arbString.arbitrary map JsString
  }

  def secureStorage(ss : SecureStorage) {
    it should "return the same data as entered" in {
      check(Prop.forAll {
        (key: String, recordName : String, json : JsValue) => {
          ss(recordName,key) = json
          val retrieved = ss(recordName,key)
          json == retrieved
        }
      })
    }

    it should "update existing records entered" in {
      check(Prop.forAll {
        (key: String, recordName : String, json : JsValue, json2 : JsValue) => {
          ss(recordName,key) = json
          ss(recordName,key) = json2          
          val retrieved = ss(recordName,key)
          json2 == retrieved
        }
      })
    }

    it should "not find a record after deleting it" in {
      check(Prop.forAll {
        (key: String, recordName : String, json : JsValue) => {
          ss(recordName,key) = json
          val fut = ss - recordName
          scala.concurrent.Await.ready(fut, 1 second)
          ss.get(recordName,key) == None
        }
      })
    }


    it should "not return data once deleted" in {
      check(Prop.forAll {
        (key: String, recordName : String, json : JsValue) => {
          ss(recordName,key) = json
          val f = ss - recordName
          scala.concurrent.Await.ready(f, 1 second)
          val retrieved = ss.get(recordName,key)
          retrieved == None
        }
      })
    }

    it should "fail to decrypt data with the wrong passphrase" in {

        ss("somerecord","correctkey") = JsArray(Seq(JsString("hiya")))
        intercept[java.lang.SecurityException] {
          ss("somerecord","wrongkey")
        }

      // The below code is more exhaustive but takes about a minute to run -

      // def genKeyPair: Gen[(String,String)] = for {
      //   key1 <- Gen.identifier
      //   key2 <- Gen.identifier
      //   if key1 != key2
      // } yield (key1,  key2)
      // implicit def arbKeyPair: Arbitrary[(String,String)] = Arbitrary(genKeyPair)

      // check(Prop.forAll { (keys: (String,String), recordName : String, json : JsValue) =>
      //   ss(recordName,keys._1) = json
      //   Prop.throws(classOf[java.lang.SecurityException]) {
      //     ss(recordName, keys._2)
      //   }
      // })

    }
  }
}

class SecureStorageActorTest extends FlatSpec with Matchers
with SecureStorageBehaviours with BeforeAndAfter {
  val system = ActorSystem("TEST", ConfigFactory.parseString("""
    akka.stdout-loglevel = "OFF"
    akka.loglevel = "OFF"
                                                             """))
  val timeout = akka.util.Timeout {
    import scala.concurrent.duration._
    5 seconds
  }

  val driver = new reactivemongo.api.MongoDriver
  val conn = driver.connection(Seq("localhost"))
  val db = conn("securestoragetest")

  val actor: SecureStorage =
    TypedActor(system).typedActorOf(TypedProps(classOf[SecureStorage],
      new SecureStorageTypedActor("PLATFORMKEY",db)), "ss")

  "A Secure Storage Dummy Mongo Implementation" should
    behave like secureStorage(actor)
}
