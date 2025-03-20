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

package controllers.btn

import base.SpecBase
import helpers.{AllMocks, TestDataFixture}
import models.btn.BTNSuccess
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import pages._
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.BTNService
import services.audit.AuditService
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.btn.{BTNCannotReturnView, CheckYourAnswersView}

import java.time.ZonedDateTime
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with TestDataFixture with AllMocks with BeforeAndAfterEach {

  override def beforeEach(): Unit =
    super.beforeEach()

  private val validUserAnswers = emptyUserAnswers
    .setOrException(EntitiesInsideOutsideUKPage, true)
    .setOrException(BTNLast4AccountingPeriodsPage, false)
    .setOrException(BTNNext2AccountingPeriodsPage, false)

  "CheckYourAnswersController" when {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(validUserAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(validUserAnswers))

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.CheckYourAnswersController.onPageLoad.url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]

        val expectedSummaryList = SummaryListViewModel(
          rows = Seq(
            SubAccountingPeriodSummary.row(someSubscriptionLocalData.subAccountingPeriod),
            BTNEntitiesInsideOutsideUKSummary.row(validUserAnswers),
            BTNLast4AccountingPeriodSummary.row(validUserAnswers),
            BTNNext2AccountingPeriodsSummary.row(validUserAnswers)
          ).flatten
        ).withCssClass("govuk-!-margin-bottom-9")

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(expectedSummaryList)(
          request,
          appConfig(application),
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      when(mockSessionRepository.get(any())) thenReturn Future.successful(None)

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.CheckYourAnswersController.onPageLoad.url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(validUserAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BTNService].toInstance(mockBTNService),
          bind[AuditService].toInstance(mockAuditService)
        )
        .build()

      when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(validUserAnswers))
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockBTNService.submitBTN(any())(any(), any())) thenReturn Future.successful(
        BTNSuccess(processingDate = ZonedDateTime.now())
      )
      when(
        mockAuditService.auditBTN(
          pillarReference = any(),
          accountingPeriod = any(),
          entitiesInsideAndOutsideUK = any(),
          apiResponseData = any()
        )(any[HeaderCarrier])
      ) thenReturn Future.successful(AuditResult.Success)

      running(application) {
        val request = FakeRequest(POST, controllers.btn.routes.CheckYourAnswersController.onSubmit.url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.btn.routes.BTNConfirmationController.onPageLoad.url
      }
    }

    "must redirect to problem with service page when submission fails" in {

      val application = applicationBuilder(userAnswers = Some(validUserAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BTNService].toInstance(mockBTNService),
          bind[AuditService].toInstance(mockAuditService)
        )
        .build()

      when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(validUserAnswers))
      when(mockBTNService.submitBTN(any())(any(), any())) thenReturn Future.failed(
        new HttpException("Failed to submit BTN", INTERNAL_SERVER_ERROR)
      )
      when(
        mockAuditService.auditBTN(
          pillarReference = any(),
          accountingPeriod = any(),
          entitiesInsideAndOutsideUK = any(),
          apiResponseData = any()
        )(any[HeaderCarrier])
      ) thenReturn Future.successful(AuditResult.Success)

      running(application) {
        val request = FakeRequest(POST, controllers.btn.routes.CheckYourAnswersController.onSubmit.url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.btn.routes.BTNProblemWithServiceController.onPageLoad.url
      }
    }

    "must return OK and the correct view for a GET of cannotReturnKnockback" in {

      val application = applicationBuilder(userAnswers = Some(validUserAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(validUserAnswers))

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.CheckYourAnswersController.cannotReturnKnockback.url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[BTNCannotReturnView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, appConfig(application), messages(application)).toString
      }
    }
  }
}
