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
import models.subscription.{AccountingPeriod, SubscriptionLocalData}
import models.{MneOrDomestic, NonUKAddress, NormalMode}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.SubscriptionService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.btn.BTNBeforeStartView

import java.time.LocalDate
import scala.concurrent.Future

class BTNBeforeStartControllerSpec extends SpecBase {
  override val mockSessionRepository:   SessionRepository   = mock[SessionRepository]
  override val mockSubscriptionService: SubscriptionService = mock[SubscriptionService]

  "Btn BeforeStart Controller" when {
    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = None).build()
      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.BTNBeforeStartController.onPageLoad().url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[BTNBeforeStartView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(NormalMode)(request, appConfig(application), messages(application)).toString
      }
    }

    "must update user answers with PLR reference when subscription data is available" in {
      val mockSessionRepository   = mock[SessionRepository]
      val mockSubscriptionService = mock[SubscriptionService]
      when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))
      when(mockSubscriptionService.getSubscriptionCache(any())(any[HeaderCarrier])) thenReturn Future.successful(
        SubscriptionLocalData(
          plrReference = "XEPLR0000000000",
          subMneOrDomestic = MneOrDomestic.Uk,
          subAccountingPeriod = AccountingPeriod(LocalDate.now(), LocalDate.now().plusYears(1)),
          subPrimaryContactName = "John Smith",
          subPrimaryEmail = "johnsmith@gmailx.com",
          subPrimaryPhonePreference = false,
          subPrimaryCapturePhone = None,
          subAddSecondaryContact = false,
          subSecondaryContactName = None,
          subSecondaryEmail = None,
          subSecondaryCapturePhone = None,
          subSecondaryPhonePreference = None,
          subRegisteredAddress = NonUKAddress("Flat 1", Some("John Street"), "London", None, Some("WC1N 1AG"), ""),
          accountStatus = None
        )
      )
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[SubscriptionService].toInstance(mockSubscriptionService)
        )
        .build()
      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.BTNBeforeStartController.onPageLoad().url)
        val result  = route(application, request).value
        status(result) mustEqual OK
        verify(mockSessionRepository).get(any())
        verify(mockSubscriptionService).getSubscriptionCache(any())(any[HeaderCarrier])
        verify(mockSessionRepository).set(any())
      }
    }

    "must handle the case when no user answers are found" in {
      val mockSessionRepository   = mock[SessionRepository]
      val mockSubscriptionService = mock[SubscriptionService]
      when(mockSessionRepository.get(any())) thenReturn Future.successful(None)
      when(mockSubscriptionService.getSubscriptionCache(any())(any[HeaderCarrier])) thenReturn Future.successful(
        SubscriptionLocalData(
          plrReference = "",
          subMneOrDomestic = MneOrDomestic.Uk,
          subAccountingPeriod = AccountingPeriod(LocalDate.now(), LocalDate.now().plusYears(1)), // Example dates
          subPrimaryContactName = "",
          subPrimaryEmail = "",
          subPrimaryPhonePreference = false,
          subPrimaryCapturePhone = None,
          subAddSecondaryContact = false,
          subSecondaryContactName = None,
          subSecondaryEmail = None,
          subSecondaryCapturePhone = None,
          subSecondaryPhonePreference = None,
          subRegisteredAddress = NonUKAddress("", None, "", None, None, ""),
          accountStatus = None
        )
      )
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val application = applicationBuilder(userAnswers = None)
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[SubscriptionService].toInstance(mockSubscriptionService)
        )
        .build()
      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.BTNBeforeStartController.onPageLoad().url)
        val result  = route(application, request).value
        status(result) mustEqual OK
        verify(mockSessionRepository).get(any())
        verify(mockSubscriptionService).getSubscriptionCache(any())(any[HeaderCarrier])
        verify(mockSessionRepository).set(any())
      }
    }
  }
}
