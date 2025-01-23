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

package controllers.uktr

import base.SpecBase
import controllers.routes
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.PlrReferencePage
import pages.agent.AgentClientPillar2ReferencePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.SubscriptionService
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import views.html.uktr.UKTaxReturnStartView

import scala.concurrent.Future

class UKTaxReturnStartControllerSpec extends SpecBase {

  "UK Tax Return Start Controller" when {

    val enrolments: Set[Enrolment] = Set(
      Enrolment(
        key = "HMRC-PILLAR2-ORG",
        identifiers = Seq(
          EnrolmentIdentifier("PLRID", "12345678"),
          EnrolmentIdentifier("UTR", "ABC12345")
        ),
        state = "activated"
      )
    )
    val plrReference = "testPlrRef"

    "must return OK and the correct view if PlrReference in session" in {
      val ua = UserAnswers(userAnswersId).setOrException(PlrReferencePage, plrReference)
      val application = applicationBuilder(Some(ua), enrolments)
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository), bind[SubscriptionService].toInstance(mockSubscriptionService))
        .build()
      when(mockSessionRepository.get(any()))
        .thenReturn(Future.successful(Some(ua)))
      when(mockSubscriptionService.readSubscription(any())(any())).thenReturn(Future.successful(subscriptionData))
      running(application) {
        val request = FakeRequest(GET, controllers.uktr.routes.UKTaxReturnStartController.onPageLoad.url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[UKTaxReturnStartView]
        status(result) mustEqual OK
        val content = contentAsString(result)
        content mustEqual view(inactiveStatus = false)(request, appConfig(application), messages(application)).toString
      }
    }

    "must return OK and the correct view if AgentClientPillar2Reference in session" in {
      val ua = UserAnswers(userAnswersId).setOrException(AgentClientPillar2ReferencePage, plrReference)
      val application = applicationBuilder(Some(ua), enrolments)
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository), bind[SubscriptionService].toInstance(mockSubscriptionService))
        .build()
      when(mockSessionRepository.get(any()))
        .thenReturn(Future.successful(Some(ua)))
      when(mockSubscriptionService.readSubscription(any())(any())).thenReturn(Future.successful(subscriptionData))
      running(application) {
        val request = FakeRequest(GET, controllers.uktr.routes.UKTaxReturnStartController.onPageLoad.url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[UKTaxReturnStartView]
        status(result) mustEqual OK
        val content = contentAsString(result)
        content mustEqual view(inactiveStatus = false)(request, appConfig(application), messages(application)).toString
      }
    }

    "must redirect to Journey Recovery Controller if no PlrReference or no AgentClientPillar2Reference in session" in {
      val application = applicationBuilder(Some(emptyUserAnswers), enrolments)
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository), bind[SubscriptionService].toInstance(mockSubscriptionService))
        .build()
      when(mockSessionRepository.get(any()))
        .thenReturn(Future.successful(Some(emptyUserAnswers)))
      when(mockSubscriptionService.readSubscription(any())(any())).thenReturn(Future.successful(subscriptionData))
      running(application) {
        val request = FakeRequest(GET, controllers.uktr.routes.UKTaxReturnStartController.onPageLoad.url)
        val result  = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.JourneyRecoveryController.onPageLoad().url)
      }
    }

  }
}
