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
import services.obligationsandsubmissions.ObligationsAndSubmissionsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.dueandoverduereturns.DueAndOverdueReturnsView

import scala.concurrent.Future

class DueAndOverdueReturnsControllerSpec extends SpecBase with MockitoSugar with ScalaFutures with DueAndOverdueReturnsDataFixture {

  // Standard application with mocked service
  lazy val application: Application = applicationBuilder(
    subscriptionLocalData = Some(someSubscriptionLocalData),
    userAnswers = Some(emptyUserAnswers)
  ).overrides(
    bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
  ).build()

  lazy val view: DueAndOverdueReturnsView = application.injector.instanceOf[DueAndOverdueReturnsView]

  "DueAndOverdueReturnsController" when {
    "onPageLoad" must {
      "return OK and display the correct view for a GET with no returns" in {
        when(mockObligationsAndSubmissionsService.handleData(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(emptyResponse))

        val request = FakeRequest(GET, controllers.dueandoverduereturns.routes.DueAndOverdueReturnsController.onPageLoad.url)
        val result  = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(emptyResponse, fromDate, toDate)(
          request,
          appConfig(application),
          messages(application)
        ).toString
      }

      "return OK and display the correct view for a GET with due returns" in {
        when(mockObligationsAndSubmissionsService.handleData(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(dueReturnsResponse))

        val request = FakeRequest(GET, controllers.dueandoverduereturns.routes.DueAndOverdueReturnsController.onPageLoad.url)
        val result  = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(dueReturnsResponse, fromDate, toDate)(
          request,
          appConfig(application),
          messages(application)
        ).toString
      }

      "return OK and display the correct view for a GET with overdue returns" in {
        when(mockObligationsAndSubmissionsService.handleData(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(overdueReturnsResponse))

        val request = FakeRequest(GET, controllers.dueandoverduereturns.routes.DueAndOverdueReturnsController.onPageLoad.url)
        val result  = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(overdueReturnsResponse, fromDate, toDate)(
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
    }
  }
}
