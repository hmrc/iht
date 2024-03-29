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

package utils

import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

/**
  * Created by yasar on 24/10/16.
  */
class HeaderDecoratorTest extends PlaySpec {

  class HeaderDecoratorUnderTest(servicesConfig: ServicesConfig) extends HeaderDecorator(servicesConfig){
    override val urlHeaderEnvironmentValue: String = "env-1"
    override val urlHeaderAuthorizationValue: String = "auth-string-1"
  }

  "header decorator" must{

    "contain correct DES value" in{

      val mockServicesConfig = mock[ServicesConfig]

      val headerDecorator = new HeaderDecoratorUnderTest(mockServicesConfig)

      val headers = headerDecorator.desExternalHttpHeaders()

      headers.size mustBe 2
      headers.find(_._1 == "Authorization").get._2 mustBe  "Bearer auth-string-1"
      headers.find(_._1 == "Environment").get._2 mustBe  "env-1"
    }

  }
}
