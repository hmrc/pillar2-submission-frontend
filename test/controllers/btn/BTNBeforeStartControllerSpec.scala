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
import controllers.actions.AgentAccessFilterAction
import models.NormalMode
import models.obligationsandsubmissions.ObligationStatus
import models.subscription.{AccountingPeriod, SubscriptionLocalData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.{PlrReferencePage, SubAccountingPeriodPage}
import play.api.Application
import play.api.inject.bind
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.obligationsandsubmissions.ObligationsAndSubmissionsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.btn.BTNBeforeStartView

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class BTNBeforeStartControllerSpec extends SpecBase {

  val plrReference = "testPlrRef"
  val dates: AccountingPeriod = AccountingPeriod(LocalDate.now, LocalDate.now.plusYears(1))

  val ua: SubscriptionLocalData =
    emptySubscriptionLocalData.setOrException(SubAccountingPeriodPage, dates).setOrException(PlrReferencePage, plrReference)

  def application: Application = applicationBuilder(subscriptionLocalData = Some(ua), userAnswers = Some(emptyUserAnswers))
    .overrides(
      bind[AgentAccessFilterAction].toInstance(mockAgentAccessFilterAction),
      bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
      bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
    )
    .build()

  "BTNBeforeStartController" when {
    "must redirect to unauthorised page when AgentAccessFilterAction returns a request block/redirect" in {
      running(application) {
        when(mockAgentAccessFilterAction.executionContext).thenReturn(scala.concurrent.ExecutionContext.global)
        when(mockAgentAccessFilterAction.filter[AnyContent](any()))
          .thenReturn(Future.successful(Some(Redirect(controllers.routes.UnauthorisedController.onPageLoad))))

        val request = FakeRequest(GET, controllers.btn.routes.BTNBeforeStartController.onPageLoad().url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.UnauthorisedController.onPageLoad.url
      }
    }

    "must allow access to start page when AgentAccessFilterAction check passes" in {
      running(application) {
        when(mockAgentAccessFilterAction.executionContext).thenReturn(scala.concurrent.ExecutionContext.global)
        when(mockAgentAccessFilterAction.filter[AnyContent](any())).thenReturn(Future.successful(None))
        when(mockSubscriptionConnector.getSubscriptionCache(any())(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(Some(someSubscriptionLocalData)))
        when(mockObligationsAndSubmissionsService.handleData(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(obligationsAndSubmissionsSuccessResponse(ObligationStatus.Open)))

        val request = FakeRequest(GET, controllers.btn.routes.BTNBeforeStartController.onPageLoad().url)
        val result  = route(application, request).value

        val view = application.injector.instanceOf[BTNBeforeStartView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(isAgent = false, hasMultipleAccountingPeriods = false, NormalMode)(
          request,
          appConfig(application),
          messages(application)
        ).toString
      }
    }

    "redirect to JourneyRecoveryController when no subscription data is found" in {
      running(application) {
        when(mockAgentAccessFilterAction.executionContext).thenReturn(scala.concurrent.ExecutionContext.global)
        when(mockAgentAccessFilterAction.filter[AnyContent](any())).thenReturn(Future.successful(None))
        when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))

        val request = FakeRequest(GET, controllers.btn.routes.BTNBeforeStartController.onPageLoad().url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
