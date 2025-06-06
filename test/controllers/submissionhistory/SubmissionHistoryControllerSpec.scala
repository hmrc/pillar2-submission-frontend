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
import models.obligationsandsubmissions.{AccountingPeriodDetails, Obligation, ObligationStatus, ObligationType}
import models.obligationsandsubmissions.ObligationStatus.Fulfilled
import models.obligationsandsubmissions.ObligationType.GIR
import models.obligationsandsubmissions.SubmissionType.UKTR_CREATE
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.obligationsandsubmissions.ObligationsAndSubmissionsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.submissionhistory.{SubmissionHistoryNoSubmissionsView, SubmissionHistoryView}

import java.time.LocalDate
import scala.concurrent.Future

class SubmissionHistoryControllerSpec extends SpecBase with MockitoSugar with ScalaFutures with SubmissionHistoryDataFixture {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, controllers.submissionhistory.routes.SubmissionHistoryController.onPageLoad.url)

  "SubmissionHistoryController" must {

    "return OK and render the SubmissionHistoryView when submissions are present" in {
      when(mockObligationsAndSubmissionsService.handleData(any[String], any[LocalDate], any[LocalDate])(any[HeaderCarrier]))
        .thenReturn(Future.successful(submissionHistoryResponse))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
        .overrides(
          bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
        )
        .build()

      running(application) {

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmissionHistoryView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(submissionHistoryResponse.accountingPeriodDetails, isAgent = false)(
          request,
          appConfig(application),
          messages(application)
        ).toString
      }
    }

    "return OK and render the SubmissionHistoryView when GIR obligation is fulfilled by BTN (no submissions)" in {
      val girObligationFulfilledByBTNResponse = obligationsAndSubmissionsSuccessResponse(
        obligationType = ObligationType.GIR,
        status = Fulfilled,
        submissionType = UKTR_CREATE
      ).success.copy(
        accountingPeriodDetails = Seq(
          AccountingPeriodDetails(
            startDate = LocalDate.now,
            endDate = LocalDate.now.plusYears(1),
            dueDate = LocalDate.now.plusYears(1),
            underEnquiry = false,
            obligations = Seq(
              Obligation(
                obligationType = ObligationType.GIR,
                status = Fulfilled,
                canAmend = true,
                submissions = Seq.empty // Empty submissions - fulfilled by BTN
              )
            )
          )
        )
      )

      when(mockObligationsAndSubmissionsService.handleData(any[String], any[LocalDate], any[LocalDate])(any[HeaderCarrier]))
        .thenReturn(Future.successful(girObligationFulfilledByBTNResponse))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
        .overrides(
          bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
        )
        .build()

      running(application) {

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmissionHistoryView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(girObligationFulfilledByBTNResponse.accountingPeriodDetails, isAgent = false)(
          request,
          appConfig(application),
          messages(application)
        ).toString
      }
    }

    "return OK and render the submissionHistoryNoSubmissionsView when no submissions are present" in {
      when(mockObligationsAndSubmissionsService.handleData(any[String], any[LocalDate], any[LocalDate])(any[HeaderCarrier]))
        .thenReturn(Future.successful(submissionHistoryResponse.copy(accountingPeriodDetails = Seq.empty)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
        .overrides(
          bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
        )
        .build()

      running(application) {

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmissionHistoryNoSubmissionsView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(isAgent = false)(request, appConfig(application), messages(application)).toString
      }
    }

    "redirect to JourneyRecoveryController on exception" in {
      when(mockObligationsAndSubmissionsService.handleData(any(), any(), any())(any()))
        .thenReturn(Future.failed(new Exception("something went wrong")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
        .overrides(
          bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
        )
        .build()

      running(application) {

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.JourneyRecoveryController.onPageLoad(None).url)
      }
    }
  }
}
