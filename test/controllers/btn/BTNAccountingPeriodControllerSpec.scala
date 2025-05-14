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
import connectors.SubscriptionConnector
import controllers.btn.routes._
import models.NormalMode
import models.obligationsandsubmissions.ObligationStatus
import models.subscription.{AccountStatus, AccountingPeriod, SubscriptionLocalData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.{PlrReferencePage, SubAccountingPeriodPage}
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import repositories.SessionRepository
import services.obligationsandsubmissions.ObligationsAndSubmissionsService
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.http.HeaderCarrier
import utils.ViewHelpers
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import views.html.btn.{BTNAccountingPeriodView, BTNReturnSubmittedView}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class BTNAccountingPeriodControllerSpec extends SpecBase {

  lazy val btnAccountingPeriodRoute: String = BTNAccountingPeriodController.onPageLoad(NormalMode).url

  val plrReference = "testPlrRef"
  val dates: AccountingPeriod = AccountingPeriod(LocalDate.now, LocalDate.now.plusYears(1))
  val dateHelper = new ViewHelpers()

  val ua: SubscriptionLocalData =
    emptySubscriptionLocalData.setOrException(SubAccountingPeriodPage, dates).setOrException(PlrReferencePage, plrReference)

  def application: Application = applicationBuilder(subscriptionLocalData = Some(ua), userAnswers = Some(emptyUserAnswers))
    .overrides(
      bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
      bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
    )
    .build()

  "BTNAccountingPeriodController" when {
    "must return OK and the correct view if PlrReference in session and obligation is not fulfilled and account status is false" in {
      val list = SummaryListViewModel(
        rows = Seq(
          SummaryListRowViewModel(
            "btn.accountingPeriod.startAccountDate",
            value = ValueViewModel(HtmlContent(HtmlFormat.escape(dateHelper.formatDateGDS(LocalDate.now))))
          ),
          SummaryListRowViewModel(
            "btn.accountingPeriod.endAccountDate",
            value = ValueViewModel(HtmlContent(HtmlFormat.escape(dateHelper.formatDateGDS(LocalDate.now.plusYears(1)))))
          )
        )
      )

      when(mockSubscriptionConnector.getSubscriptionCache(any())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(someSubscriptionLocalData)))

      when(mockObligationsAndSubmissionsService.handleData(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(obligationsAndSubmissionsSuccessResponse(ObligationStatus.Open)))

      running(application) {
        val request = FakeRequest(GET, btnAccountingPeriodRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[BTNAccountingPeriodView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(list, NormalMode, appConfig.changeAccountingPeriodUrl)(
          request,
          appConfig(application),
          messages(application)
        ).toString
      }
    }

    "must redirect to a knockback page when a BTN is submitted" in {
      val application = applicationBuilder(subscriptionLocalData = Some(ua), userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(submittedBTNRecord))

      running(application) {
        val request = FakeRequest(GET, btnAccountingPeriodRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CheckYourAnswersController.cannotReturnKnockback.url
      }
    }

    "must redirect to error page when account status inactive flag is true" in {
      val ua = emptySubscriptionLocalData
        .copy(accountStatus = Some(AccountStatus(true)))
        .setOrException(SubAccountingPeriodPage, dates)
        .setOrException(PlrReferencePage, plrReference)
      val application = applicationBuilder(subscriptionLocalData = Some(ua))
        .overrides(
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
        )
        .build()

      when(mockSubscriptionConnector.getSubscriptionCache(any())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(someSubscriptionLocalData.copy(accountStatus = Some(AccountStatus(true))))))

      when(mockObligationsAndSubmissionsService.handleData(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(obligationsAndSubmissionsSuccessResponse(ObligationStatus.Fulfilled)))

      running(application) {
        val request = FakeRequest(GET, btnAccountingPeriodRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad(None).url
      }
    }

    "must redirect to BTN specific error page when subscription data is not returned" in {
      val application = applicationBuilder(subscriptionLocalData = None, userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, btnAccountingPeriodRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.btn.routes.BTNProblemWithServiceController.onPageLoad.url
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSubscriptionConnector.getSubscriptionCache(any())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(someSubscriptionLocalData)))

      running(application) {
        val request = FakeRequest(POST, BTNAccountingPeriodController.onSubmit(NormalMode).url)
        val result  = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual BTNEntitiesInUKOnlyController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to the next page when valid data is submitted with UkOther" in {
      when(mockSubscriptionConnector.getSubscriptionCache(any())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(someSubscriptionLocalDataUkOther)))

      running(application) {
        val request = FakeRequest(POST, BTNAccountingPeriodController.onSubmit(NormalMode).url)
        val result  = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual BTNEntitiesInUKOnlyController.onPageLoad(NormalMode).url
      }
    }

    "must return OK and the correct view for return submitted page" in {
      val list = SummaryListViewModel(
        rows = Seq(
          SummaryListRowViewModel(
            "btn.returnSubmitted.startAccountDate",
            value = ValueViewModel(HtmlContent(HtmlFormat.escape(dateHelper.formatDateGDS(LocalDate.now))))
          ),
          SummaryListRowViewModel(
            "btn.returnSubmitted.endAccountDate",
            value = ValueViewModel(HtmlContent(HtmlFormat.escape(dateHelper.formatDateGDS(LocalDate.now.plusYears(1)))))
          )
        )
      )

      when(mockSubscriptionConnector.getSubscriptionCache(any())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(someSubscriptionLocalData)))

      when(mockObligationsAndSubmissionsService.handleData(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(obligationsAndSubmissionsSuccessResponse(ObligationStatus.Fulfilled)))

      running(application) {
        val request = FakeRequest(GET, btnAccountingPeriodRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[BTNReturnSubmittedView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(list)(request, appConfig(application), messages(application)).toString
      }
    }
  }
}
