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

import play.api.Logger
import uk.gov.hmrc.crypto._
import com.google.common.io.BaseEncoding.base64

/**
  * Encrypts and decrypts data using the Rijndael cypher in
  * galois/counter mode operation. It accepts a platform key and then
  * an instance key. The two keys are then hashed together to provide
  * the symmetric key.
  *
  * {{{
  * // Create an instance of AES using my platform key
  * val aes = new AES("first part to the key")
  *
  * // Encrypt some data
  * val instanceKey = "second part to the key"
  * val message = "mySecretMessage"
  * val encryptedMessage = aes.encrypt(message, instanceKey)
  *
  * // Decrypt some data
  * assert(aes.decrypt(encryptedMessage, instanceKey) == message)
  * }}}
  */
final class AES(@transient val platformKey : String) extends AESEncryption

trait AESEncryption {

  protected val platformKey : String

  def decrypt(encryptedData: Array[Byte], secondKey : String): String =
    cypher(secondKey).decrypt(new Crypted(new String(encryptedData))).value

  def encrypt(plainData: String, secondKey : String): Array[Byte] =
    cypher(secondKey).encrypt(new PlainText(plainData)).value.getBytes

  private def cypher(secondKey : String) = {
    Logger.debug("platform key : " + platformKey)
    val keyHash = hashIt(platformKey, secondKey)
    CompositeSymmetricCrypto.aesGCM(keyHash, Nil)
  }

  private def hashIt(args : String*) : String = {
    val md = java.security.MessageDigest.getInstance("MD5")
    args.map(_.getBytes("UTF-8")).foreach(md.update(_))
    new String(base64.encode(md.digest))
  }
}
