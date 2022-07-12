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

package metrics

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer.Context
import com.kenshoo.play.metrics.Metrics
import javax.inject.Inject
import models.enums.Api
import models.enums.Api.Api

/**
 *
 * Created by Vineet Tyagi on 25/09/15.
 *
 * Creates the metrics to get it linked with Grafana.Can use the metric name ex  - caseList-response-timer
 * to see the stats in Grafana.
 */

class MicroserviceMetricsImpl @Inject()(val metrics: Metrics) extends MicroserviceMetrics

trait MicroserviceMetrics {
  val metrics: Metrics
  val registry: MetricRegistry = metrics.defaultRegistry

  val timers = Map(
    Api.GET_CASE_LIST -> registry.timer("caseList-response-timer"),
    Api.GET_CASE_DETAILS -> registry.timer("caseDetails-response-timer"),
    Api.GET_PROBATE_DETAILS -> registry.timer("probateDetails-response-timer"),
    Api.GET_APPLICATION_DETAILS -> registry.timer("applicationDetails-response-timer"),
    Api.SUB_REGISTRATION -> registry.timer("subRegistration-response-timer"),
    Api.SUB_REAL_TIME_RISKING -> registry.timer("subRealTimeRisking-response-timer"),
    Api.SUB_APPLICATION -> registry.timer("subApplication-response-timer"),
    Api.SUB_REQUEST_CLEARANCE -> registry.timer("subReqClearance-response-timer")
  )

  val successCounters = Map(
    Api.GET_CASE_LIST -> registry.counter("caseList-success-counter"),
    Api.GET_CASE_DETAILS -> registry.counter("caseDetails-success-counter"),
    Api.GET_PROBATE_DETAILS -> registry.counter("probateDetails-success-counter"),
    Api.GET_APPLICATION_DETAILS -> registry.counter("applicationDetails-success-counter"),
    Api.SUB_REGISTRATION -> registry.counter("subRegistration-success-counter"),
    Api.SUB_REAL_TIME_RISKING -> registry.counter("subRealTimeRisking-success-counter"),
    Api.SUB_APPLICATION -> registry.counter("subApplication-success-counter"),
    Api.SUB_REQUEST_CLEARANCE -> registry.counter("subReqClearance-success-counter")
  )

  val failedCounters = Map(

    Api.GET_CASE_LIST -> registry.counter("caseList-failed-counter"),
    Api.GET_CASE_DETAILS -> registry.counter("caseDetails-failed-counter"),
    Api.GET_PROBATE_DETAILS -> registry.counter("probateDetails-failed-counter"),
    Api.GET_APPLICATION_DETAILS -> registry.counter("applicationDetails-failed-counter"),
    Api.SUB_REGISTRATION -> registry.counter("subRegistration-failed-counter"),
    Api.SUB_REAL_TIME_RISKING -> registry.counter("subRealTimeRisking-failed-counter"),
    Api.SUB_APPLICATION -> registry.counter("subApplication-failed-counter"),
    Api.SUB_REQUEST_CLEARANCE -> registry.counter("subReqClearance-failed-counter")
  )

  def startTimer(api: Api): Context = timers(api).time()

  def incrementSuccessCounter(api: Api): Unit = successCounters(api).inc()

  def incrementFailedCounter(api: Api): Unit = failedCounters(api).inc()
}
