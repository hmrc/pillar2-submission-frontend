/*
 * Copyright 2024 HM Revenue & Customs
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
import connectors.ObligationConnector
import models.obligation.ObligationStatus.Fulfilled
import models.obligation.{ObligationInformation, ObligationType}
import models.{InternalIssueError, ObligationNotFoundError}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class ObligationServiceSpec extends SpecBase {

  val obligationData: ObligationInformation = ObligationInformation(ObligationType.UKTR, Fulfilled, LocalDate.now(), LocalDate.now(), LocalDate.now())

  "ObligationService" must {

    "return obligation when the connector returns valid data and transformation is successful" in {

      val application = applicationBuilder()
        .overrides(
          bind[ObligationConnector].toInstance(mockObligationConnector)
        )
        .build()

      running(application) {
        when(mockObligationConnector.getObligation(any(), any(), any())(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(Some(obligationData)))

        val service: ObligationService = application.injector.instanceOf[ObligationService]
        val result = service.handleObligation(PlrReference, LocalDate.now(), LocalDate.now()).futureValue

        result mustBe Right(obligationData.status)
      }
    }

    "return ObligationNotFoundError when the connector returns None" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val application = applicationBuilder()
        .overrides(
          bind[ObligationConnector].toInstance(mockObligationConnector)
        )
        .build()

      running(application) {
        when(mockObligationConnector.getObligation(any(), any(), any())(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(None))

        val service: ObligationService = application.injector.instanceOf[ObligationService]
        val result = service.handleObligation(PlrReference, LocalDate.now(), LocalDate.now()).futureValue

        result mustBe Left(ObligationNotFoundError)
      }
    }

    "return InternalIssueError when connector fails" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val application = applicationBuilder()
        .overrides(
          bind[ObligationConnector].toInstance(mockObligationConnector)
        )
        .build()

      running(application) {
        when(mockObligationConnector.getObligation(any(), any(), any())(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val service: ObligationService = application.injector.instanceOf[ObligationService]
        val result = service.handleObligation(PlrReference, LocalDate.now(), LocalDate.now()).futureValue

        result mustBe Left(InternalIssueError)
      }
    }

  }
}
