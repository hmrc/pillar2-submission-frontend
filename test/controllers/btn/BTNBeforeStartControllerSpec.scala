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
import models.NormalMode
import models.requests.OptionalDataRequest
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.btn.BTNBeforeStartView

class BTNBeforeStartControllerSpec extends SpecBase {

  "BTNBeforeStartController" when {

    "must return OK and the correct group view for a GET when agent journey enabled" in {

      val application = applicationBuilder(userAnswers = None, additionalData = Map("features.asaAccessEnabled" -> true)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.BTNBeforeStartController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[BTNBeforeStartView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(isAgent = false, NormalMode)(request, appConfig(application), messages(application)).toString
      }
    }

    "must return OK and the correct group view for a GET when agent journey disabled" in {

      val application = applicationBuilder(userAnswers = None, additionalData = Map("features.asaAccessEnabled" -> false)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.BTNBeforeStartController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[BTNBeforeStartView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(isAgent = false, NormalMode)(request, appConfig(application), messages(application)).toString
      }
    }

    "must return OK and the correct agent view for a GET when agent journey enabled" in {

      val application = applicationBuilder(userAnswers = None, additionalData = Map("features.asaAccessEnabled" -> true)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.BTNBeforeStartController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[BTNBeforeStartView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(isAgent = true, NormalMode)(request, appConfig(application), messages(application)).toString
      }
    }

    "must redirect an agent to unauthorised page when agent journey disabled" in {

      val application = applicationBuilder(userAnswers = None, additionalData = Map("features.asaAccessEnabled" -> false)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.BTNBeforeStartController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.UnauthorisedController.onPageLoad.url
      }
    }
  }
}
