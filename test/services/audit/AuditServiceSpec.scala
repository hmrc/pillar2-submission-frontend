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

package services.audit

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.Future

class AuditServiceSpec extends SpecBase {

  override val mockAuditConnector: AuditConnector = mock[AuditConnector]

  val application = applicationBuilder()
    .overrides(
      bind[AuditConnector].toInstance(mockAuditConnector)
    )
    .build()

  "AuditService" must {
    "return Success when audit call is successful" in {
      running(application) {
        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        val service = application.injector.instanceOf[AuditService]
        val result = service
          .auditBTN(
            pillarReference = "PLR1234567890",
            accountingPeriod = "2024-03-20",
            entitiesInsideAndOutsideUK = true,
            apiResponseData = models.audit.ApiResponseData(
              statusCode = 200,
              processingDate = "2024-03-20T07:32:03Z",
              errorCode = None,
              responseMessage = "Success"
            )
          )(hc)
          .futureValue

        result mustBe AuditResult.Success
      }
    }

    "return Disabled when audit connector is disabled" in {
      running(application) {
        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Disabled))

        val service = application.injector.instanceOf[AuditService]
        val result = service
          .auditBTN(
            pillarReference = "PLR1234567890",
            accountingPeriod = "2024-03-20",
            entitiesInsideAndOutsideUK = true,
            apiResponseData = models.audit.ApiResponseData(
              statusCode = 200,
              processingDate = "2024-03-20T07:32:03Z",
              errorCode = None,
              responseMessage = "Success"
            )
          )(hc)
          .futureValue

        result mustBe AuditResult.Disabled
      }
    }

    "return Failure when audit connector returns failure" in {
      running(application) {
        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Failure("Audit failure")))

        val service = application.injector.instanceOf[AuditService]
        val result = service
          .auditBTN(
            pillarReference = "PLR1234567890",
            accountingPeriod = "2024-03-20",
            entitiesInsideAndOutsideUK = true,
            apiResponseData = models.audit.ApiResponseData(
              statusCode = 200,
              processingDate = "2024-03-20T07:32:03Z",
              errorCode = None,
              responseMessage = "Success"
            )
          )(hc)
          .futureValue

        result mustBe AuditResult.Failure("Audit failure")
      }
    }

    "propagate exceptions from audit connector" in {
      running(application) {
        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Test exception")))

        val service = application.injector.instanceOf[AuditService]
        val resultFuture = service.auditBTN(
          pillarReference = "PLR1234567890",
          accountingPeriod = "2024-03-20",
          entitiesInsideAndOutsideUK = true,
          apiResponseData = models.audit.ApiResponseData(
            statusCode = 200,
            processingDate = "2024-03-20T07:32:03Z",
            errorCode = None,
            responseMessage = "Success"
          )
        )(hc)

        whenReady(resultFuture.failed) { exception =>
          exception mustBe a[RuntimeException]
          exception.getMessage mustBe "Test exception"
        }
      }
    }
  }
}
