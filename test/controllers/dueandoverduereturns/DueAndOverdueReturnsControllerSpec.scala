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

package controllers.dueandoverduereturns

import base.SpecBase
import connectors.SubscriptionConnector
import controllers.{routes => baseRoutes}
import helpers.DueAndOverdueReturnsDataFixture
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SubscriptionService
import services.obligationsandsubmissions.ObligationsAndSubmissionsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.dueandoverduereturns.DueAndOverdueReturnsView

import scala.concurrent.Future

class DueAndOverdueReturnsControllerSpec extends SpecBase with MockitoSugar with ScalaFutures with DueAndOverdueReturnsDataFixture {

  lazy val application: Application = applicationBuilder(
    subscriptionLocalData = Some(someSubscriptionLocalData),
    userAnswers = Some(emptyUserAnswers)
  ).overrides(
    bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService),
    bind[SubscriptionService].toInstance(mockSubscriptionService)
  ).build()

  lazy val view: DueAndOverdueReturnsView = application.injector.instanceOf[DueAndOverdueReturnsView]

  "DueAndOverdueReturnsController" when {
    "onPageLoad" must {
      "return OK and display the correct view for a GET with no returns" in {
        when(mockObligationsAndSubmissionsService.handleData(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(emptyResponse))
        when(mockSubscriptionService.getSubscriptionCache(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(someSubscriptionLocalData))

        val request = FakeRequest(GET, controllers.dueandoverduereturns.routes.DueAndOverdueReturnsController.onPageLoad.url)
        val result  = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(emptyResponse, fromDate, toDate, false)(
          request,
          appConfig(application),
          messages(application)
        ).toString
      }

      "return OK and display the correct view for a GET with due returns" in {
        when(mockObligationsAndSubmissionsService.handleData(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(dueReturnsResponse))
        when(mockSubscriptionService.getSubscriptionCache(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(someSubscriptionLocalData))

        val request = FakeRequest(GET, controllers.dueandoverduereturns.routes.DueAndOverdueReturnsController.onPageLoad.url)
        val result  = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(dueReturnsResponse, fromDate, toDate, false)(
          request,
          appConfig(application),
          messages(application)
        ).toString
      }

      "return OK and display the correct view for a GET with overdue returns" in {
        when(mockObligationsAndSubmissionsService.handleData(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(overdueReturnsResponse))
        when(mockSubscriptionService.getSubscriptionCache(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(someSubscriptionLocalData))

        val request = FakeRequest(GET, controllers.dueandoverduereturns.routes.DueAndOverdueReturnsController.onPageLoad.url)
        val result  = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(overdueReturnsResponse, fromDate, toDate, false)(
          request,
          appConfig(application),
          messages(application)
        ).toString
      }

      "redirect to Journey Recovery for a GET if the service call fails" in {
        when(mockObligationsAndSubmissionsService.handleData(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("Test exception")))

        val request = FakeRequest(GET, controllers.dueandoverduereturns.routes.DueAndOverdueReturnsController.onPageLoad.url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual baseRoutes.JourneyRecoveryController.onPageLoad().url
      }

      "display agent-specific content when isAgent is true" in {

        val emptyContent = contentAsString(
          view(emptyResponse, fromDate, toDate, true)(
            FakeRequest(),
            appConfig(application),
            messages(application)
          )
        )

        val dueContent = contentAsString(
          view(dueReturnsResponse, fromDate, toDate, true)(
            FakeRequest(),
            appConfig(application),
            messages(application)
          )
        )

        emptyContent must include("Your client is up to date with their returns for this accounting period")
        dueContent   must include("If your client has multiple returns due, they will be separated by accounting periods")
        dueContent   must include("You must submit each return before its due date using your client’s commercial software supplier")
      }

      "must redirect to general error page when subscription data is not returned" in {
        val application = applicationBuilder(subscriptionLocalData = None, userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
            bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, controllers.dueandoverduereturns.routes.DueAndOverdueReturnsController.onPageLoad.url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad(None).url
        }
      }
    }
  }
}
