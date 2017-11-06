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

package connectors.securestorage

import akka.actor.{ ActorContext, TypedActor, TypedProps }
import akka.dispatch._
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Json.{parse,prettyPrint}
import play.api.libs.json._
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.json.BSONFormats._
import scala.concurrent._
import scala.concurrent.duration._

/**
  * Stores to MongoDB with data encoded in a single field
  *
  * Please be aware that given we do not control how mongo purges data
  * this could potentially result in data being recoverable after it
  * has been "deleted" as we cannot overwrite with zeros/random data.
  *
  */
class SecureStorageTypedActor(
  @transient val platformKey : String,
  val db : DefaultDB
) extends SecureStorage with AESEncryption {
  implicit val ec : ExecutionContext = TypedActor.dispatcher

  val collection = db(this.getClass.getSimpleName)

  private def getBson(id: String) = collection.find(
    BSONDocument("id" -> id)
  ).cursor[BSONDocument].headOption

  def getAsync(id: String, key: String): Future[JsValue] = {
    getBson(id).flatMap {
      _ match {
        case Some(bson) => Future{
          val top = BSONDocumentFormat.writes(bson).as[JsObject]
          val dataAsString = decrypt(
            (top \\ "data").head.asInstanceOf[JsString].value.getBytes,
            key
          )
          Json.parse(dataAsString)
        }
        case None => Future.failed(
          new NoSuchElementException(s"key not found $id")
        )
      }
    }
  }

  def updateAsync(id: String, key: String, v: JsValue) : Future[Any] = {
    val date = new org.joda.time.DateTime
    collection.update(
      selector = BSONDocument("id" -> id),
      update = BSONDocument(
        "$setOnInsert" -> BSONDocument(
          "id" -> id,
          "createdOn" -> BSONDateTime(date.getMillis)
        ),
        "$set" -> BSONDocument(
          "updatedOn" -> BSONDateTime(date.getMillis),
          "data" -> new String(encrypt(Json.prettyPrint(v), key))
        )
      ),
      upsert = true
    )
  }

  def clean(olderThan : DateTime) {
    collection.remove(
      BSONDocument("createdOn" -> BSONDocument(
        "$lt" -> BSONDateTime(olderThan.getMillis)
      )),
      firstMatchOnly=false)
  }

  //scalastyle:off method.name
  def -(id : String) : Future[Any] = {
    collection.remove(
      BSONDocument("id" -> id),
      firstMatchOnly=false
    )
  }
  //scalastyle:on
}
