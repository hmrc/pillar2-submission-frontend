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
import connectors.BtnConnector
import models.InternalIssueError
import models.btn.BtnRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import services.BtnServiceSpec._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class BtnServiceSpec extends SpecBase {

  val btnSuccessfulResponseFuture: Future[HttpResponse] = Future.successful(btnSuccessfulHttpResponse)

  "BtnService" must {
    "return status=201 when Btn connector returns valid data" in {
      val application = applicationBuilder()
        .overrides(
          bind[BtnConnector].toInstance(mockBtnConnector)
        )
        .build()
      running(application) {
        when(mockBtnConnector.submitBtn(any(), any())(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(btnSuccessfulHttpResponse))

        val service: BtnService = application.injector.instanceOf[BtnService]
        val result = service.submitBtn(btnRequestBodyDefaultAccountingPeriodDates, plrReferenceForValidResponse).futureValue
        result.status mustBe CREATED
        result.json mustBe jsonBtnSuccessfulResponse
      }
    }

    "return InternalIssueError when Btn connector returns InternalIssueError" in {
      val application = applicationBuilder()
        .overrides(
          bind[BtnConnector].toInstance(mockBtnConnector)
        )
        .build()
      running(application) {
        when(mockBtnConnector.submitBtn(any(), any())(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.failed(InternalIssueError))
        val service: BtnService = application.injector.instanceOf[BtnService]
        val result = service.submitBtn(btnRequestBodyDefaultAccountingPeriodDates, plrReferenceForValidResponse).failed.futureValue
        result mustBe InternalIssueError
      }
    }

    "throw an Exception when Btn connector throws a RuntimeException" in {
      val application = applicationBuilder()
        .overrides(
          bind[BtnConnector].toInstance(mockBtnConnector)
        )
        .build()
      running(application) {
        when(mockBtnConnector.submitBtn(any(), any())(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.failed(new RuntimeException("runtime error")))
        val service: BtnService = application.injector.instanceOf[BtnService]
        val result = service.submitBtn(btnRequestBodyDefaultAccountingPeriodDates, plrReferenceForValidResponse).failed.futureValue
        result shouldBe a[RuntimeException]
      }
    }
  }
}

object BtnServiceSpec {
  val btnRequestBodyDefaultAccountingPeriodDates: BtnRequest = BtnRequest(
    accountingPeriodFrom = LocalDate.now.minusYears(1),
    accountingPeriodTo = LocalDate.now
  )
  val plrReferenceForValidResponse = "XEPLR0000000000"
  val btnSuccessfulResponseJsonString: String =
    """{"processingDate":"2025-01-10T16:54:26Z",
      | "formBundleNumber":"11223344556677",
      | "chargeReference":"XTC01234123412"}""".stripMargin
  val jsonBtnSuccessfulResponse: JsValue = Json.parse(btnSuccessfulResponseJsonString)
  val btnSuccessfulHttpResponse: HttpResponse = new HttpResponse {
    override def status:  Int                      = CREATED
    override def body:    String                   = btnSuccessfulResponseJsonString
    override def json:    JsValue                  = jsonBtnSuccessfulResponse
    override def headers: Map[String, Seq[String]] = Map("Content-Type" -> Seq("application/json"))
  }
}
