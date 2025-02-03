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

package controllers.obligationsandsubmissions

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.obligationsandsubmissions.ObligationsAndSubmissionsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ObligationsAndSubmissionsControllerSpec extends SpecBase with MockitoSugar with ScalaFutures {

  val application: Application = applicationBuilder(subscriptionLocalData = Some(someSubscriptionLocalData))
    .overrides(bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService))
    .build()

  val requestUrl: String = routes.ObligationsAndSubmissionsController.onPageLoad(localDateFrom.toString, localDateTo.toString).url

  "onPageLoad" should {
    "must redirect to UnderConstruction page when valid dates are provided and service call is successful" in {
      when(mockObligationsAndSubmissionsService.handleData(any(), any())(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(obligationsAndSubmissionsSuccessResponse().success))

      val request = FakeRequest(GET, requestUrl)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.routes.UnderConstructionController.onPageLoad.url
    }

    "must handle invalid date format appropriately" in {
      val request = FakeRequest(GET, routes.ObligationsAndSubmissionsController.onPageLoad(localDateFrom.toString, "invalid date").url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
    }

    "must handle service failure appropriately" in {
      when(mockObligationsAndSubmissionsService.handleData(any(), any())(any[HeaderCarrier], any[String]))
        .thenReturn(Future.failed(new RuntimeException))

      val request = FakeRequest(GET, requestUrl)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
    }
  }
}
