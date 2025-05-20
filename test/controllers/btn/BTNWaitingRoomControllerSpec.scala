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
import models.btn.BTNStatus
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.btn.BTNWaitingRoomView

class BTNWaitingRoomControllerSpec extends SpecBase with MockitoSugar {

  override val mockSessionRepository: SessionRepository = mock[SessionRepository]

  "BTNWaitingRoomController" when {

    ".onPageLoad" should {

      "redirect to confirmation page when status is submitted and no submission is initiated" in {
        val userAnswers = emptyUserAnswers.set(BTNStatus, BTNStatus.submitted).get
        val application = applicationBuilder(userAnswers = Some(userAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.BTNWaitingRoomController.onPageLoad.url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.BTNConfirmationController.onPageLoad.url
        }
      }

      "redirect to confirmation page when status is submitted and minimum wait time has passed" in {
        val userAnswers = emptyUserAnswers.set(BTNStatus, BTNStatus.submitted).get
        val application = applicationBuilder(userAnswers = Some(userAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {

          val timestamp = System.currentTimeMillis() - 5000
          val request = FakeRequest(GET, routes.BTNWaitingRoomController.onPageLoad.url)
            .withSession(
              "btn_submission_initiated" -> "true",
              "btn_submission_timestamp" -> timestamp.toString
            )
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.BTNConfirmationController.onPageLoad.url
        }
      }

      "show waiting room when status is submitted but minimum wait time hasn't passed" in {
        val userAnswers = emptyUserAnswers.set(BTNStatus, BTNStatus.submitted).get
        val application = applicationBuilder(userAnswers = Some(userAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {

          val timestamp = System.currentTimeMillis() - 1000
          val request = FakeRequest(GET, routes.BTNWaitingRoomController.onPageLoad.url)
            .withSession(
              "btn_submission_initiated" -> "true",
              "btn_submission_timestamp" -> timestamp.toString
            )
          val result = route(application, request).value
          val view   = application.injector.instanceOf[BTNWaitingRoomView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view()(request, appConfig(application), messages(application)).toString
        }
      }

      "redirect to error page when status is error and no submission is initiated" in {
        val userAnswers = emptyUserAnswers.set(BTNStatus, BTNStatus.error).get
        val application = applicationBuilder(userAnswers = Some(userAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.BTNWaitingRoomController.onPageLoad.url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.BTNProblemWithServiceController.onPageLoad.url
        }
      }

      "show waiting room page when status is processing" in {
        val userAnswers = emptyUserAnswers.set(BTNStatus, BTNStatus.processing).get
        val application = applicationBuilder(userAnswers = Some(userAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.BTNWaitingRoomController.onPageLoad.url)
            .withSession("btn_submission_initiated" -> "true")
          val result = route(application, request).value
          val view   = application.injector.instanceOf[BTNWaitingRoomView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view()(request, appConfig(application), messages(application)).toString
        }
      }

      "redirect to check your answers when status is missing" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.BTNWaitingRoomController.onPageLoad.url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.CheckYourAnswersController.onPageLoad.url
        }
      }
    }
  }
}
