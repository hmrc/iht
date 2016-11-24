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

package metrics

import com.codahale.metrics.Timer
import com.codahale.metrics.Timer.Context
import com.kenshoo.play.metrics.MetricsRegistry
import models.enums.Api
import models.enums.Api.Api

/**
 *
 * Created by Vineet Tyagi on 25/09/15.
 *
 * Creates the metrics to get it linked with Grafana.Can use the metric name ex  - caseList-response-timer
 * to see the stats in Grafana.
 */

trait Metrics {
  def startTimer(api: Api): Timer.Context
  def incrementSuccessCounter(api: Api): Unit
  def incrementFailedCounter(api: Api): Unit
}

object Metrics extends Metrics{

  val timers = Map(
    Api.GET_CASE_LIST -> MetricsRegistry.defaultRegistry.timer("caseList-response-timer"),
    Api.GET_CASE_DETAILS -> MetricsRegistry.defaultRegistry.timer("caseDetails-response-timer"),
    Api.GET_PROBATE_DETAILS -> MetricsRegistry.defaultRegistry.timer("probateDetails-response-timer"),
    Api.GET_APPLICATION_DETAILS -> MetricsRegistry.defaultRegistry.timer("applicationDetails-response-timer"),
    Api.SUB_REGISTRATION -> MetricsRegistry.defaultRegistry.timer("subRegistration-response-timer"),
    Api.SUB_REAL_TIME_RISKING -> MetricsRegistry.defaultRegistry.timer("subRealTimeRisking-response-timer"),
    Api.SUB_APPLICATION -> MetricsRegistry.defaultRegistry.timer("subApplication-response-timer"),
    Api.SUB_REQUEST_CLEARANCE -> MetricsRegistry.defaultRegistry.timer("subReqClearance-response-timer")
  )

  val successCounters = Map(
    Api.GET_CASE_LIST -> MetricsRegistry.defaultRegistry.counter("caseList-success-counter"),
    Api.GET_CASE_DETAILS -> MetricsRegistry.defaultRegistry.counter("caseDetails-success-counter"),
    Api.GET_PROBATE_DETAILS -> MetricsRegistry.defaultRegistry.counter("probateDetails-success-counter"),
    Api.GET_APPLICATION_DETAILS -> MetricsRegistry.defaultRegistry.counter("applicationDetails-success-counter"),
    Api.SUB_REGISTRATION -> MetricsRegistry.defaultRegistry.counter("subRegistration-success-counter"),
    Api.SUB_REAL_TIME_RISKING -> MetricsRegistry.defaultRegistry.counter("subRealTimeRisking-success-counter"),
    Api.SUB_APPLICATION -> MetricsRegistry.defaultRegistry.counter("subApplication-success-counter"),
    Api.SUB_REQUEST_CLEARANCE -> MetricsRegistry.defaultRegistry.counter("subReqClearance-success-counter")
  )

  val failedCounters = Map(

    Api.GET_CASE_LIST -> MetricsRegistry.defaultRegistry.counter("caseList-failed-counter"),
    Api.GET_CASE_DETAILS -> MetricsRegistry.defaultRegistry.counter("caseDetails-failed-counter"),
    Api.GET_PROBATE_DETAILS -> MetricsRegistry.defaultRegistry.counter("probateDetails-failed-counter"),
    Api.GET_APPLICATION_DETAILS -> MetricsRegistry.defaultRegistry.counter("applicationDetails-failed-counter"),
    Api.SUB_REGISTRATION -> MetricsRegistry.defaultRegistry.counter("subRegistration-failed-counter"),
    Api.SUB_REAL_TIME_RISKING -> MetricsRegistry.defaultRegistry.counter("subRealTimeRisking-failed-counter"),
    Api.SUB_APPLICATION -> MetricsRegistry.defaultRegistry.counter("subApplication-failed-counter"),
    Api.SUB_REQUEST_CLEARANCE -> MetricsRegistry.defaultRegistry.counter("subReqClearance-failed-counter")
  )

  override def startTimer(api: Api): Context = timers(api).time()

  override def incrementSuccessCounter(api: Api): Unit = successCounters(api).inc()

  override def incrementFailedCounter(api: Api): Unit = failedCounters(api).inc()
}
