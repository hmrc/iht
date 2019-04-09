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

import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, _}
import reactivemongo.api.{DefaultDB, FailoverStrategy}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, _}
import reactivemongo.core.nodeset.Authenticate
import reactivemongo.play.json.ImplicitBSONHandlers._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

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
  val db: DefaultDB,
  auth: Option[Authenticate]
) extends SecureStorage with AESEncryption {

  val collection: BSONCollection = db.collection(this.getClass.getSimpleName)

  auth map { a =>
    db.connection.authenticate(db.name, a.user, a.password.get, failoverStrategy = FailoverStrategy.default)
  }

  private def getBson(id: String)(implicit ec: ExecutionContext) = collection.find(BSONDocument("id" -> id)).one[BSONDocument]

  def getAsync(id: String, key: String)(implicit ec: ExecutionContext): Future[JsValue] = {
    getBson(id).flatMap {
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

  def updateAsync(id: String, key: String, v: JsValue)(implicit ec: ExecutionContext) : Future[Any] = {
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

  def clean(olderThan : DateTime)(implicit ec: ExecutionContext) {
    collection.remove(
      BSONDocument("createdOn" -> BSONDocument(
        "$lt" -> BSONDateTime(olderThan.getMillis)
      )),
      firstMatchOnly=false)
  }

  //scalastyle:off method.name
  def -(id : String)(implicit ec: ExecutionContext) : Future[Any] = {
    collection.remove(
      BSONDocument("id" -> id),
      firstMatchOnly=false
    )
  }
  //scalastyle:on
}
