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
import connectors._
import models.InternalIssueError
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.inject.bind
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionServiceSpec extends SpecBase {

  lazy val currentDate: LocalDate = LocalDate.now()

  val id           = "testId"
  val plrReference = "testPlrRef"

  "SubscriptionService" must {

    "readSubscription" when {

      "return SubscriptionDAta object when the connector returns valid data and transformation is successful" in {

        val application = applicationBuilder()
          .overrides(
            bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
          )
          .build()
        running(application) {
          when(mockSubscriptionConnector.readSubscription(any())(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some(subscriptionData)))
          val service: SubscriptionService = application.injector.instanceOf[SubscriptionService]
          val result = service.readSubscription("plr").futureValue

          result mustBe subscriptionData
        }
      }

      "return InternalIssueError when the connector returns None" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val application = applicationBuilder()
          .overrides(
            bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
          )
          .build()
        running(application) {
          when(mockSubscriptionConnector.readSubscription(any())(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(None))
          val service: SubscriptionService = application.injector.instanceOf[SubscriptionService]
          val result = service.readSubscription("plr").failed.futureValue

          result mustBe models.InternalIssueError
        }
      }

      "handle exceptions thrown by the connector" in {

        val application = applicationBuilder()
          .overrides(
            bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
          )
          .build()

        running(application) {
          when(mockSubscriptionConnector.readSubscription(any())(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.failed(new RuntimeException("Connection error")))
          val service: SubscriptionService = application.injector.instanceOf[SubscriptionService]
          val resultFuture = service.readSubscription("plr")

          resultFuture.failed.futureValue shouldBe a[RuntimeException]
        }
      }

      "return getSubscriptionCache object when the connector returns valid data and transformation is successful" in {

        val application = applicationBuilder()
          .overrides(
            bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
          )
          .build()
        running(application) {
          when(mockSubscriptionConnector.getSubscriptionCache(any())(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.successful(Some(someSubscriptionLocalData)))
          val service: SubscriptionService = application.injector.instanceOf[SubscriptionService]
          val result = service.getSubscriptionCache("userid").futureValue

          result mustBe someSubscriptionLocalData
        }
      }

      "return InternalIssueError when the connector for getSubscriptionCache" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val application = applicationBuilder()
          .overrides(
            bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
          )
          .build()
        running(application) {
          when(mockSubscriptionConnector.getSubscriptionCache(any())(any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.failed(InternalIssueError))
          val service: SubscriptionService = application.injector.instanceOf[SubscriptionService]
          val result = service.getSubscriptionCache("userid").failed.futureValue
          result mustBe models.InternalIssueError
        }
      }

    }
  }
}
