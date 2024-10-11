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
import forms.BtnEntitiesBothInUKAndOutsideFormProvider
import models.{MneOrDomestic, NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.EntitiesBothInUKAndOutsidePage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.btn.{BtnAmendDetailsView, BtnEntitiesBothInUKAndOutsideView}

import scala.concurrent.Future
import play.api.data.Form

class BtnEntitiesBothInUKAndOutsideControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val formProvider = new BtnEntitiesBothInUKAndOutsideFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val entitiesBothInUKAndOutsideRoute: String = controllers.btn.routes.BtnEntitiesBothInUKAndOutsideController.onPageLoad(NormalMode).url

  "EntitiesBothInUKAndOutside Controller" when {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, entitiesBothInUKAndOutsideRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[BtnEntitiesBothInUKAndOutsideView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, appConfig(application), messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(EntitiesBothInUKAndOutsidePage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, entitiesBothInUKAndOutsideRoute)

        val view = application.injector.instanceOf[BtnEntitiesBothInUKAndOutsideView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode)(request, appConfig(application), messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, entitiesBothInUKAndOutsideRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.btn.routes.BtnRevenues750In2AccountingPeriodController.onPageLoad(NormalMode).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, entitiesBothInUKAndOutsideRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[BtnEntitiesBothInUKAndOutsideView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, appConfig(application), messages(application)).toString
      }
    }

    "must return OK and the correct view for amend group details page" in {

      val application = applicationBuilder(subscriptionLocalData = Some(emptySubscriptionLocalData)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.BtnEntitiesBothInUKAndOutsideController.onPageLoadAmendGroupDetails().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[BtnAmendDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(MneOrDomestic.Uk)(request, appConfig(application), messages(application)).toString
      }
    }

    "redirect to journey recovery if MNE details are not present amend group details page" in {

      val application = applicationBuilder(subscriptionLocalData = None).build()

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.BtnEntitiesBothInUKAndOutsideController.onPageLoadAmendGroupDetails().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  }
}
