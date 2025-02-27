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
import controllers.btn.routes._
import forms.BTNNext2AccountingPeriodsFormProvider
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.BTNNext2AccountingPeriodsPage
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.btn.BTNNext2AccountingPeriodsView

import scala.concurrent.Future

class BTNNext2AccountingPeriodsControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")
  def application(implicit userAnswers: UserAnswers = emptyUserAnswers): Application =
    applicationBuilder(Some(userAnswers)).build()

  val formProvider = new BTNNext2AccountingPeriodsFormProvider()
  val form:                                Form[Boolean] = formProvider()
  lazy val btnNext2AccountingPeriodsRoute: String        = BTNNext2AccountingPeriodsController.onPageLoad(NormalMode).url

  "BTNNext2AccountingPeriodsController" when {
    "must return OK and the correct view for a GET" in {
      running(application) {
        val request = FakeRequest(GET, btnNext2AccountingPeriodsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[BTNNext2AccountingPeriodsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, appConfig(application), messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      implicit val userAnswers: UserAnswers = UserAnswers(userAnswersId).set(BTNNext2AccountingPeriodsPage, true).success.value

      running(application) {
        val request = FakeRequest(GET, btnNext2AccountingPeriodsRoute)
        val view    = application.injector.instanceOf[BTNNext2AccountingPeriodsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode)(request, appConfig(application), messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      running(application) {
        val request =
          FakeRequest(POST, btnNext2AccountingPeriodsRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CheckYourAnswersController.onPageLoad.url
      }
    }

    "must redirect to a knockback page when the answer is true" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(false)

      running(application) {
        val request =
          FakeRequest(POST, btnNext2AccountingPeriodsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual BTNNext2AccountingPeriodsController.submitUKTRKnockback.url
      }
    }

    "must redirect to a knockback page when a BTN is submitted" in {
      when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(submittedBTNRecord))

      val application = applicationBuilder()
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, btnNext2AccountingPeriodsRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CheckYourAnswersController.cannotReturnKnockback.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      running(application) {
        val request =
          FakeRequest(POST, btnNext2AccountingPeriodsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))
        val view      = application.injector.instanceOf[BTNNext2AccountingPeriodsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, appConfig(application), messages(application)).toString
      }
    }
  }
}
