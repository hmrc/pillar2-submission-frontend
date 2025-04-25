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
import controllers.actions.AgentAccessFilterAction
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Application
import play.api.inject.bind
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class BTNBeforeStartControllerSpec extends SpecBase {

  def application: Application = applicationBuilder()
    .overrides(bind[AgentAccessFilterAction].toInstance(mockAgentAccessFilterAction))
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

    "must redirect to start page when AgentAccessFilterAction check passes" in {
      running(application) {
        when(mockAgentAccessFilterAction.executionContext).thenReturn(scala.concurrent.ExecutionContext.global)
        when(mockAgentAccessFilterAction.filter[AnyContent](any())).thenReturn(Future.successful(None))

        val request = FakeRequest(GET, controllers.btn.routes.BTNBeforeStartController.onPageLoad().url)
        val result  = route(application, request).value

        status(result) mustEqual OK
        // add more checks ...
      }
    }
  }
}
