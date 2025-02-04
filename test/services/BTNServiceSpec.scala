/*
 * Copyright 2025 HM Revenue & Customs
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

package services

import base.SpecBase
import connectors.BTNConnector
import models.InternalIssueError
import models.btn.{BTNRequest, BTNSuccess}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import services.BTNServiceSpec._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}
import scala.concurrent.{ExecutionContext, Future}

class BTNServiceSpec extends SpecBase {
  implicit val pillar2Id:          String          = pillar2IdForValidResponse
  val btnSuccessfulResponseFuture: Future[JsValue] = Future.successful(jsonBTNSuccess)

  val application: Application = applicationBuilder()
    .overrides(
      bind[BTNConnector].toInstance(mockBTNConnector)
    )
    .build()

  "BTNService" must {
    "return status=201/CREATED when BTN connector returns valid data" in {
      running(application) {
        when(mockBTNConnector.submitBTN(any())(any[HeaderCarrier], any(), any[ExecutionContext]))
          .thenReturn(Future.successful(jsonBTNSuccess))
        val service: BTNService = application.injector.instanceOf[BTNService]
        val result = service.submitBTN(btnRequestBodyDefaultAccountingPeriodDates).futureValue
        result mustBe BTNSuccess(zonedTestDateTime)
      }
    }

    "return InternalIssueError when BTN connector returns InternalIssueError" in {
      running(application) {
        when(mockBTNConnector.submitBTN(any())(any[HeaderCarrier], any(), any[ExecutionContext]))
          .thenReturn(Future.failed(InternalIssueError))
        val service: BTNService = application.injector.instanceOf[BTNService]
        val failure = service.submitBTN(btnRequestBodyDefaultAccountingPeriodDates).failed.futureValue
        failure mustBe InternalIssueError
      }
    }

    "throw an Exception when BTN connector throws a RuntimeException" in {
      running(application) {
        when(mockBTNConnector.submitBTN(any())(any[HeaderCarrier], any(), any[ExecutionContext]))
          .thenReturn(Future.failed(new RuntimeException("runtime error")))
        val service: BTNService = application.injector.instanceOf[BTNService]
        val failure = service.submitBTN(btnRequestBodyDefaultAccountingPeriodDates).failed.futureValue
        failure mustBe InternalIssueError
      }
    }

    "handle exceptions other than InternalIssueError" in {
      running(application) {
        when(mockBTNConnector.submitBTN(any())(any[HeaderCarrier], any(), any[ExecutionContext]))
          .thenReturn(Future.failed(new IllegalArgumentException("Test exception")))
        val service: BTNService = application.injector.instanceOf[BTNService]
        val failure = service.submitBTN(btnRequestBodyDefaultAccountingPeriodDates).failed.futureValue
        failure mustBe InternalIssueError
      }
    }
    "handle any other unexpected error" in {
      running(application) {
        when(mockBTNConnector.submitBTN(any())(any[HeaderCarrier], any(), any[ExecutionContext]))
          .thenReturn(Future.failed(new Error("Unexpected error")))
        val service: BTNService = application.injector.instanceOf[BTNService]
        val failure = service.submitBTN(btnRequestBodyDefaultAccountingPeriodDates).failed.futureValue
        failure mustBe InternalIssueError
      }
    }
  }
}

object BTNServiceSpec {
  val btnRequestBodyDefaultAccountingPeriodDates: BTNRequest = BTNRequest(
    accountingPeriodFrom = LocalDate.now.minusYears(1),
    accountingPeriodTo = LocalDate.now
  )
  val pillar2IdForValidResponse = "XEPLR0000000000"
  val testZonedDateTime         = "2025-01-10T16:54:26Z"
  val datePattern               = DateTimeFormatter.ISO_DATE_TIME
  val zonedTestDateTime: ZonedDateTime = ZonedDateTime.parse("2025-01-10T16:54:26Z", datePattern)

  val btnSuccessJsonString: String     = s"""{"processingDate":"$testZonedDateTime"}"""
  val btnSuccessJson:       JsValue    = Json.parse(btnSuccessJsonString)
  val btnSuccess:           BTNSuccess = btnSuccessJson.as[BTNSuccess]

  val jsonBTNSuccess: JsValue = Json.parse(btnSuccessJsonString)
}
