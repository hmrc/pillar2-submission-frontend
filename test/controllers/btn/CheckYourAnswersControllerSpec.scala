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
import controllers.{btn, routes}
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages._
import play.api.Application
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.{BTNService, BTNServiceSpec}
import views.html.btn.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {
  def application: Application = applicationBuilder(subscriptionLocalData = Some(someSubscriptionLocalData))
    .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
    .build()

  def request(ua: UserAnswers = validBtnCyaUa): FakeRequest[AnyContentAsEmpty.type] = {
    when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(ua)))
    FakeRequest(GET, btn.routes.CheckYourAnswersController.onPageLoad.url)
  }

  val view: CheckYourAnswersView = application.injector.instanceOf[CheckYourAnswersView]

  "CheckYourAnswersController" when {
    ".onPageLoad" should {
      "must return OK and the correct view for a GET" in {
        val result = route(application, request()).value
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(btnCyaSummaryList)(request(), appConfig(application), messages(application)).toString
      }

      "must redirect to IndexController on disqualifying answers" in {
        val emptyUa = validBtnCyaUa.setOrException(EntitiesBothInUKAndOutsidePage, false)
        val result  = route(application, request(emptyUa)).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.IndexController.onPageLoad.url
      }

      "must redirect to JourneyRecoveryController on retrieval of answers failure" in {
        val application = applicationBuilder(subscriptionLocalData = Some(someSubscriptionLocalData)).build()
        val result      = route(application, request()).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    ".onSubmit" should {
      "must redirect to BTN Confirmation page on submission" in {
        val applicationBTN: Application = applicationBuilder(subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(
            bind[BTNService].toInstance(mockBTNService)
          )
          .build()

        when(mockBTNService.submitBTN(any())(any(), any())).thenReturn(Future.successful(BTNServiceSpec.btnSuccessfulHttpResponse))

        running(applicationBTN) {
          val requestBTN = FakeRequest(POST, controllers.btn.routes.CheckYourAnswersController.onSubmit.url)
          val result     = route(applicationBTN, requestBTN).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.btn.routes.BTNConfirmationController.onPageLoad.url
        }
      }
    }
  }
}
