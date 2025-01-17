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
import models.btn.BTNRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import services.BTNServiceSpec._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class BTNServiceSpec extends SpecBase {
  implicit val pillar2Id:          String               = pillar2IdForValidResponse
  val btnSuccessfulResponseFuture: Future[HttpResponse] = Future.successful(btnSuccessfulHttpResponse)
  val application: Application = applicationBuilder()
    .overrides(
      bind[BTNConnector].toInstance(mockBTNConnector)
    )
    .build()

  "BTNService" must {
    "return status=201 when BTN connector returns valid data" in {
      running(application) {
        when(mockBTNConnector.submitBTN(any())(any[HeaderCarrier], any(), any[ExecutionContext]))
          .thenReturn(Future.successful(btnSuccessfulHttpResponse))
        val service: BTNService = application.injector.instanceOf[BTNService]
        val result = service.submitBTN(btnRequestBodyDefaultAccountingPeriodDates).futureValue
        result.status mustBe CREATED
        result.json mustBe jsonBTNSuccessfulResponse
      }
    }

    "return InternalIssueError when BTN connector returns InternalIssueError" in {
      running(application) {
        when(mockBTNConnector.submitBTN(any())(any[HeaderCarrier], any(), any[ExecutionContext]))
          .thenReturn(Future.failed(InternalIssueError))
        val service: BTNService = application.injector.instanceOf[BTNService]
        val result = service.submitBTN(btnRequestBodyDefaultAccountingPeriodDates).failed.futureValue
        result mustBe InternalIssueError
      }
    }

    "throw an Exception when BTN connector throws a RuntimeException" in {
      running(application) {
        when(mockBTNConnector.submitBTN(any())(any[HeaderCarrier], any(), any[ExecutionContext]))
          .thenReturn(Future.failed(new RuntimeException("runtime error")))
        val service: BTNService = application.injector.instanceOf[BTNService]
        val result = service.submitBTN(btnRequestBodyDefaultAccountingPeriodDates).failed.futureValue
        result shouldBe a[RuntimeException]
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
  val btnSuccessfulResponseJsonString: String =
    """{"processingDate":"2025-01-10T16:54:26Z",
      | "formBundleNumber":"11223344556677",
      | "chargeReference":"XTC01234123412"}""".stripMargin
  val jsonBTNSuccessfulResponse: JsValue = Json.parse(btnSuccessfulResponseJsonString)
  val btnSuccessfulHttpResponse: HttpResponse = new HttpResponse {
    override def status:  Int                      = CREATED
    override def body:    String                   = btnSuccessfulResponseJsonString
    override def json:    JsValue                  = jsonBTNSuccessfulResponse
    override def headers: Map[String, Seq[String]] = Map("Content-Type" -> Seq("application/json"))
  }
}
