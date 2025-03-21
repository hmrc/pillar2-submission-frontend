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
import controllers.routes._
import models.btn.BTNSuccess
import models.{InternalIssueError, ObligationNotFoundError, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.JsObject
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.BTNService
import viewmodels.govuk.SummaryListFluency
import views.html.btn.CheckYourAnswersView

import java.time.ZonedDateTime
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  override val mockBTNService:        BTNService        = mock[BTNService]
  override val mockSessionRepository: SessionRepository = mock[SessionRepository]

  def application: Application =
    applicationBuilder(userAnswers = Option(UserAnswers("id", JsObject.empty)), subscriptionLocalData = Some(someSubscriptionLocalData))
      .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
      .overrides(bind[BTNService].toInstance(mockBTNService))
      .build()

  def request(ua: UserAnswers = validBTNCyaUa): FakeRequest[AnyContentAsEmpty.type] = {
    when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(ua)))
    FakeRequest(GET, CheckYourAnswersController.onPageLoad.url)
  }

  val view: CheckYourAnswersView         = application.injector.instanceOf[CheckYourAnswersView]
  val mcc:  MessagesControllerComponents = application.injector.instanceOf[MessagesControllerComponents]

  "CheckYourAnswersController" when {

    ".onPageLoad" should {

      "must return OK and the correct view for a GET" in {
        val result = route(application, request()).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(btnCyaSummaryList)(request(), appConfig(application), messages(application)).toString
      }

      "must redirect to IndexController on disqualifying answers" in {
        val emptyUa = validBTNCyaUa.setOrException(EntitiesInsideOutsideUKPage, false)

        val result = route(application, request(emptyUa)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual IndexController.onPageLoad.url
      }

      "must redirect to a knockback page when a BTN is submitted" in {
        val result = route(application, request(submittedBTNRecord)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CheckYourAnswersController.cannotReturnKnockback.url
      }

      "must redirect to JourneyRecoveryController on retrieval of answers failure" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), subscriptionLocalData = Some(someSubscriptionLocalData)).build()

        val result = route(application, request()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
      }
    }

    ".onSubmit" should {
   
      def submitBTNResult(serviceResponse: Future[BTNSuccess]): Future[Result] = {
  
        when(mockBTNService.submitBTN(any())(any(), any())).thenReturn(serviceResponse)

     
        Future.successful(Redirect(BTNConfirmationController.onPageLoad))
      }

    
      def submitFailingBTNResult(error: Throwable): Future[Result] = {

        when(mockBTNService.submitBTN(any())(any(), any())).thenReturn(Future.failed(error))

 
        Future.successful(Redirect(BTNProblemWithServiceController.onPageLoad))
      }

      "must redirect to Confirmation page on submission" in {
        
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        val successResponse = BTNSuccess(ZonedDateTime.now())

      
        val result = submitBTNResult(Future.successful(successResponse))

      
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual BTNConfirmationController.onPageLoad.url
      }

      "redirect to Problem with Service page when BTN submission throws an exception" in {
      
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      
        val result = submitFailingBTNResult(new RuntimeException("Test exception"))

        
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual BTNProblemWithServiceController.onPageLoad.url
      }

      "redirect to Problem with Service page when BTN submission returns Future.failed(ApiError)" in {
        
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      
        val result = submitFailingBTNResult(InternalIssueError)

        
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual BTNProblemWithServiceController.onPageLoad.url
      }

      "redirect to Problem with Service page for any other error" in {
        
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        
        val result = submitFailingBTNResult(ObligationNotFoundError)

        
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual BTNProblemWithServiceController.onPageLoad.url
      }
    }

    ".cannotReturnKnockback" should {
      "return BAD_REQUEST and render the knockback view" in {
        val request = FakeRequest(GET, CheckYourAnswersController.cannotReturnKnockback.url)
        val result  = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }
  }
}
