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
import models.subscription.AccountingPeriod
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.{PlrReferencePage, SubAccountingPeriodPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.test.Helpers._
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.http.HeaderCarrier
import utils.ViewHelpers
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import views.html.btn.BtnAccountingPeriodView

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class BtnAccountingPeriodControllerSpec extends SpecBase {

  "UK Tax Return Start Controller" when {

    val plrReference = "testPlrRef"
    val dates        = AccountingPeriod(LocalDate.now, LocalDate.now.plusYears(1))
    val dateHelper   = new ViewHelpers()

    "must return OK and the correct view if PlrReference in session" in {
      val list = SummaryListViewModel(
        rows = Seq(
          SummaryListRowViewModel(
            "btn.btnAccountingPeriod.startAccountDate",
            value = ValueViewModel(HtmlContent(HtmlFormat.escape(dateHelper.formatDateGDS(LocalDate.now))))
          ),
          SummaryListRowViewModel(
            "btn.btnAccountingPeriod.endAccountDate",
            value = ValueViewModel(HtmlContent(HtmlFormat.escape(dateHelper.formatDateGDS(LocalDate.now.plusYears(1)))))
          )
        )
      )
      val ua = emptySubscriptionLocalData.setOrException(SubAccountingPeriodPage, dates).setOrException(PlrReferencePage, plrReference)

      when(mockSubscriptionConnector.getSubscriptionCache(any())(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(Some(someSubscriptionLocalData)))

      val application = applicationBuilder(subscriptionLocalData = Some(ua))
        .overrides(
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
        )
        .build()
      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.BtnAccountingPeriodController.onPageLoad(NormalMode).url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[BtnAccountingPeriodView]
        status(result) mustEqual OK
        val content = contentAsString(result)
        content mustEqual view(list, NormalMode, appConfig.changeAccountingPeriodUrl)(request, appConfig(application), messages(application)).toString
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
        val request = FakeRequest(POST, controllers.btn.routes.BtnAccountingPeriodController.onSubmit(NormalMode).url)
        val result  = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.btn.routes.BtnEntitiesInUKOnlyController.onPageLoad(NormalMode).url
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
        val request = FakeRequest(POST, controllers.btn.routes.BtnAccountingPeriodController.onSubmit(NormalMode).url)
        val result  = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.btn.routes.BtnEntitiesInUKOnlyController.onPageLoad(NormalMode).url
      }
    }

  }
}
