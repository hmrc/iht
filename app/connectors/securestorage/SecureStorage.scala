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

import scala.concurrent._
import scala.concurrent.duration._
import play.api.libs.json._
import scala.util._
import java.util._
import utils.CommonHelper._

/**
  * A basic interface for the secure storage mechanism. At
  * the moment we do not know how the secure storage will be
  * implemented but this interface should permit developers to work
  * as-is. This presents an idiomatic scala interface similar to a
  * mutable MapLike except without iteration capabilities (these are
  * not supported by KeyStore/Save for later).
  *
  * {{{
  * // Storing some JSON
  * SecureStorage("mykey","mysalt") = myJson
  *
  * // Retrieve some JSON
  * SecureStorage("mykey","mysalt")
  *
  * // Retrieve some JSON as an Option
  * SecureStorage.get("mykey","mysalt")
  *
  * // Retrieve some JSON asynchronously
  * SecureStorage.getAsync("mykey","mysalt")
  *
  * // Get an object storage
  * case class Blah(t : Seq[String])
  * implicit val formatter = Json.format[Blah]
  * val os = SecureStorage.objectStorage(classOf[Blah])
  * os("myblah","mykey") = Blah(Seq("one","two"))
  * val ret = os("myblah","mykey")
  * }}}
  */
trait SecureStorage {

  /**
    * Maximum permissible time to wait for the Secure Storage system
    * to respond when requesting synchronously
    */
  var delay : Duration = Duration.Inf
  val platformKey : String
  val platformPrevKey : Seq[String] = Nil

  /**
    * Trigger the destruction of old records
    */
  def clean(olderThan : org.joda.time.DateTime)(implicit ec: ExecutionContext) : Unit

  /**
    * Insert a new JSON value into Secure Storage.
    */
  def update(id : String, key : String, v : JsValue)(implicit ec: ExecutionContext): Unit = {
    val async: Future[Any] = updateAsync(id, key, v)
    Await.result[Any](async, delay)
  }

  /**
    * Insert a new JSON value into Secure Storage without blocking.
    */
  def updateAsync(id : String, key : String, v : JsValue)(implicit ec: ExecutionContext) : Future[Any]

  /**
    * Retrieve a JSON record from secure storage
    */
  def get(id : String, key : String)(implicit ec: ExecutionContext) : Option[JsValue] = {
    val async = getAsync(id, key)
    Await.ready(async, delay)
    getOrException(async.value) match {
      case Success(x) => Some(x)
      case Failure(e:NoSuchElementException) => None
      case Failure(e) => throw e
    }
  }

  /**
    * Retrieve a JSON record from secure storage asynchronously
    */
  def getAsync(id : String, key : String)(implicit ec: ExecutionContext) : Future[JsValue]

  /**
    * Retrieve a JSON record from secure storage
    */
  def apply(id : String, key : String)(implicit ec: ExecutionContext) = getOrException(get(id, key))

  protected class SecureStorageObjectInterface[T]
    (se : SecureStorage, clazz : Class[T])
    (implicit f : Format[T], ec : ExecutionContext)
  {

    /**
      * Insert a new object into Secure Storage.
      */
    def update(id : String, key : String, v : T) {
      se.update(id, clazz.getName ++ key, Json.toJson(v)(f))
    }

    /**
      * Insert a new object into Secure Storage without blocking
      */
    def updateAsync(id : String, key : String, v : T): Future[Any] = {
      se.updateAsync(id, clazz.getName ++ key, Json.toJson(v)(f))
    }

    /**
      * Retrieve an object from secure storage
      */
    def get(id : String, key : String) : Option[T] =
      se.get(id, clazz.getName ++ key).map{
        x => Json.fromJson[T](x).getOrElse(throw new RuntimeException("Bad data"))
      }

    /**
      * Retrieve an object from secure storage asynchronously
      */
    def getAsync(id : String, key : String) : Future[T] =
      se.getAsync(id, clazz.getName ++ key).map {
        x => Json.fromJson[T](x).getOrElse(throw new RuntimeException("Bad data"))
      }

    /**
      * Retrieve an object from secure storage
      */
    def apply(id : String, key : String) = getOrException(get(id, key))
  }

  /**
    * Get an object handler which will automatically serialize and
    * deserialize your objects to JSON
    */
  def objectStorage[T](clazz : Class[T])(implicit f : Format[T], ec : ExecutionContext) =
    new SecureStorageObjectInterface[T](this, clazz)

  //scalastyle:off method.name
  def -(id : String)(implicit ec: ExecutionContext) : Future[Any]
  //scalastyle:on
}
