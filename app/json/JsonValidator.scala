/*
 * Copyright 2022 HM Revenue & Customs
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

package json

import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchemaFactory
import play.api.libs.json.JsValue

trait JsonValidator {

  def validate(jsonInstance: JsValue, schemaPath: String) = {
    val json: JsonNode = JsonLoader.fromString(jsonInstance.toString())
    val factory = JsonSchemaFactory.byDefault.getJsonSchema(JsonLoader.fromResource(schemaPath))
    factory.validate(json)
  }
}

object JsonValidator extends JsonValidator
