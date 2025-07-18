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

package controllers.submissionhistory

import base.SpecBase
import controllers.helpers.SubmissionHistoryDataFixture
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
import views.html.submissionhistory.{SubmissionHistoryNoSubmissionsView, SubmissionHistoryView}

import java.time.LocalDate
import scala.concurrent.Future

class SubmissionHistoryControllerSpec extends SpecBase with MockitoSugar with ScalaFutures with SubmissionHistoryDataFixture {

  lazy val application: Application =
    applicationBuilder(userAnswers = Some(emptyUserAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
      .overrides(
        bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService),
        bind[SubscriptionService].toInstance(mockSubscriptionService)
      )
      .build()

  lazy val view:              SubmissionHistoryView              = application.injector.instanceOf[SubmissionHistoryView]
  lazy val noSubmissionsView: SubmissionHistoryNoSubmissionsView = application.injector.instanceOf[SubmissionHistoryNoSubmissionsView]

  "SubmissionHistoryController" must {

    "return OK and render the SubmissionHistoryView when submissions are present" in {
      when(mockObligationsAndSubmissionsService.handleData(any[String], any[LocalDate], any[LocalDate])(any[HeaderCarrier]))
        .thenReturn(Future.successful(submissionHistoryResponse))
      when(mockSubscriptionService.getSubscriptionCache(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(someSubscriptionLocalData))

      val request = FakeRequest(GET, controllers.submissionhistory.routes.SubmissionHistoryController.onPageLoad.url)

      val result = route(application, request).value

      status(result) mustEqual OK

      contentAsString(result) mustEqual view(submissionHistoryResponse.accountingPeriodDetails, isAgent = false)(
        request,
        appConfig(application),
        messages(application)
      ).toString
    }

    "return OK and render the submissionHistoryNoSubmissionsView when no submissions are present" in {
      when(mockObligationsAndSubmissionsService.handleData(any[String], any[LocalDate], any[LocalDate])(any[HeaderCarrier]))
        .thenReturn(Future.successful(submissionHistoryResponse.copy(accountingPeriodDetails = Seq.empty)))
      when(mockSubscriptionService.getSubscriptionCache(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(someSubscriptionLocalData))

      val request = FakeRequest(GET, controllers.submissionhistory.routes.SubmissionHistoryController.onPageLoad.url)

      val result = route(application, request).value

      status(result) mustEqual OK

      contentAsString(result) mustEqual noSubmissionsView(isAgent = false)(request, appConfig(application), messages(application)).toString
    }

    "redirect to JourneyRecoveryController on exception" in {
      when(mockObligationsAndSubmissionsService.handleData(any(), any(), any())(any()))
        .thenReturn(Future.failed(new Exception("something went wrong")))

      val request = FakeRequest(GET, controllers.submissionhistory.routes.SubmissionHistoryController.onPageLoad.url)

      val result = route(application, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.JourneyRecoveryController.onPageLoad(None).url)
    }
  }
}
