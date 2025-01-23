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
import models.NormalMode
import models.obligation.ObligationStatus.{Fulfilled, Open}
import models.subscription.{AccountStatus, AccountingPeriod}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.{PlrReferencePage, SubAccountingPeriodPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import services.ObligationService
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.http.HeaderCarrier
import utils.ViewHelpers
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import views.html.btn.{BTNAccountingPeriodView, BTNReturnSubmittedView}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class BTNAccountingPeriodControllerSpec extends SpecBase {

  "UK Tax Return Start Controller" when {

    val plrReference = "testPlrRef"
    val dates        = AccountingPeriod(LocalDate.now, LocalDate.now.plusYears(1))
    val dateHelper   = new ViewHelpers()

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
      val ua = emptySubscriptionLocalData.setOrException(SubAccountingPeriodPage, dates).setOrException(PlrReferencePage, plrReference)

      val application = applicationBuilder(subscriptionLocalData = Some(ua))
        .overrides(
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      when(mockSubscriptionConnector.getSubscriptionCache(any())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(someSubscriptionLocalData)))

      when(mockObligationService.handleObligation(any(), any(), any())(any()))
        .thenReturn(Future.successful(Right(Open)))

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.BTNAccountingPeriodController.onPageLoad(NormalMode).url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[BTNAccountingPeriodView]
        status(result) mustEqual OK
        val content = contentAsString(result)
        content mustEqual view(list, NormalMode, appConfig.changeAccountingPeriodUrl)(request, appConfig(application), messages(application)).toString
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
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      when(mockSubscriptionConnector.getSubscriptionCache(any())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(someSubscriptionLocalData.copy(accountStatus = Some(AccountStatus(true))))))

      when(mockObligationService.handleObligation(any(), any(), any())(any()))
        .thenReturn(Future.successful(Right(Fulfilled)))

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.BTNAccountingPeriodController.onPageLoad(NormalMode).url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad(None).url
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val ua = emptySubscriptionLocalData.setOrException(SubAccountingPeriodPage, dates).setOrException(PlrReferencePage, plrReference)

      when(mockSubscriptionConnector.getSubscriptionCache(any())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(someSubscriptionLocalData)))

      val application = applicationBuilder(subscriptionLocalData = Some(ua))
        .overrides(
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
        )
        .build()
      running(application) {
        val request = FakeRequest(POST, controllers.btn.routes.BTNAccountingPeriodController.onSubmit(NormalMode).url)
        val result  = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.btn.routes.BTNEntitiesInUKOnlyController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to the next page when valid data is submitted with UkOther" in {

      val ua = emptySubscriptionLocalData.setOrException(SubAccountingPeriodPage, dates).setOrException(PlrReferencePage, plrReference)

      when(mockSubscriptionConnector.getSubscriptionCache(any())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(someSubscriptionLocalDataUkOther)))

      val application = applicationBuilder(subscriptionLocalData = Some(ua))
        .overrides(
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
        )
        .build()
      running(application) {
        val request = FakeRequest(POST, controllers.btn.routes.BTNAccountingPeriodController.onSubmit(NormalMode).url)
        val result  = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.btn.routes.BTNEntitiesInUKOnlyController.onPageLoad(NormalMode).url
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
      val ua = emptySubscriptionLocalData.setOrException(SubAccountingPeriodPage, dates).setOrException(PlrReferencePage, plrReference)

      val application = applicationBuilder(subscriptionLocalData = Some(ua))
        .overrides(
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      when(mockSubscriptionConnector.getSubscriptionCache(any())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(someSubscriptionLocalData)))

      when(mockObligationService.handleObligation(any(), any(), any())(any()))
        .thenReturn(Future.successful(Right(Fulfilled)))

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.BTNAccountingPeriodController.onPageLoad(NormalMode).url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[BTNReturnSubmittedView]
        status(result) mustEqual OK
        val content = contentAsString(result)
        content mustEqual view(list)(request, appConfig(application), messages(application)).toString
      }
    }
  }
}
