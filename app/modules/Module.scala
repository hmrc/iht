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

package modules

import com.google.inject.AbstractModule
import config.{ApplicationGlobal, ApplicationStart, ErrorHandler}
import connectors.{IhtConnector, IhtConnectorImpl}
import controllers.application.{ApplicationController, ApplicationControllerImpl}
import controllers.estateReports.{YourEstateReportsController, YourEstateReportsControllerImpl}
import controllers.registration.{RegistrationController, RegistrationControllerImpl}
import metrics.{MicroserviceMetrics, MicroserviceMetricsImpl}
import play.api.http.DefaultHttpErrorHandler
import services.{AuditService, AuditServiceImpl}
import utils.{RegistrationHelper, RegistrationHelperImpl}

class Module extends AbstractModule {
  def configure() = {
    bind(classOf[ApplicationGlobal]).to(classOf[ApplicationStart]).asEagerSingleton
    bind(classOf[DefaultHttpErrorHandler]).to(classOf[ErrorHandler]).asEagerSingleton
    bind(classOf[MicroserviceMetrics]).to(classOf[MicroserviceMetricsImpl]).asEagerSingleton
    bind(classOf[AuditService]).to(classOf[AuditServiceImpl]).asEagerSingleton
    bind(classOf[IhtConnector]).to(classOf[IhtConnectorImpl]).asEagerSingleton
    bind(classOf[RegistrationHelper]).to(classOf[RegistrationHelperImpl]).asEagerSingleton
    bind(classOf[YourEstateReportsController]).to(classOf[YourEstateReportsControllerImpl]).asEagerSingleton
    bind(classOf[ApplicationController]).to(classOf[ApplicationControllerImpl]).asEagerSingleton
    bind(classOf[RegistrationController]).to(classOf[RegistrationControllerImpl]).asEagerSingleton
  }
}
