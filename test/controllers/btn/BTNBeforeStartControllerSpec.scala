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
import config.FrontendAppConfig
import controllers.actions.{AgentAccessFilterAction, IdentifierAction}
import models.NormalMode
import models.requests.IdentifierRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.{ActionFilter, AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.btn.BTNBeforeStartView
import play.api.inject.bind

import scala.concurrent.Future

class BTNBeforeStartControllerSpec extends SpecBase {

  "BTNBeforeStartController" when {

    "must redirect to unauthorised page when AgentAccessFilterAction returns a request block/redirect" in {
      val application = applicationBuilder()
        .overrides(
          bind[AgentAccessFilterAction].toInstance(mockAgentAccessFilterAction)
        )
        .build()

      running(application) {
//        when(mockFrontendAppConfig.asaAccessEnabled).thenReturn(false)
//        when(mockAgentAccessFilterAction.filter(any[IdentifierRequest[AnyContent]]))
//          .thenReturn(Future.successful(Some(Redirect(controllers.routes.UnauthorisedController.onPageLoad))))

        when(mockAgentAccessFilterAction.refine(any[IdentifierRequest[AnyContent]]))
          .thenReturn(Future.successful(Some(Redirect(controllers.routes.UnauthorisedController.onPageLoad))))

        val request = FakeRequest(GET, controllers.btn.routes.BTNBeforeStartController.onPageLoad().url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.UnauthorisedController.onPageLoad.url
      }
    }
  }
}
