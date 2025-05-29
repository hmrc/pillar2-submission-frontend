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
import models.InternalIssueError
import models.UserAnswers
import models.btn.{BTNStatus, BTNSuccess}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.BTNService
import services.audit.AuditService
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import viewmodels.govuk.SummaryListFluency
import views.html.btn.CheckYourAnswersView

import java.time.ZonedDateTime
import scala.concurrent.{Future, Promise}

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  override val mockBTNService:        BTNService        = mock[BTNService]
  override val mockSessionRepository: SessionRepository = mock[SessionRepository]
  override val mockAuditService:      AuditService      = mock[AuditService]

  def application: Application =
    applicationBuilder(userAnswers = Option(UserAnswers("id", JsObject.empty)), subscriptionLocalData = Some(someSubscriptionLocalData))
      .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
      .overrides(bind[BTNService].toInstance(mockBTNService))
      .overrides(bind[AuditService].toInstance(mockAuditService))
      .build()

  val view: CheckYourAnswersView = application.injector.instanceOf[CheckYourAnswersView]

  "CheckYourAnswersController" when {

    ".onPageLoad" should {

      "must return OK and the correct view for a GET" in {

        when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(validBTNCyaUa)))

        val application = applicationBuilder(userAnswers = Some(validBTNCyaUa), subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          val request = FakeRequest(GET, CheckYourAnswersController.onPageLoad.url)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[CheckYourAnswersView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(btnCyaSummaryList)(request, appConfig(application), messages(application)).toString
        }
      }

      "must redirect to IndexController on disqualifying answers" in {
        val emptyUa = validBTNCyaUa.setOrException(EntitiesInsideOutsideUKPage, false)

        when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(emptyUa)))

        val application = applicationBuilder(userAnswers = Some(emptyUa), subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          val request = FakeRequest(GET, CheckYourAnswersController.onPageLoad.url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual IndexController.onPageLoad.url
        }
      }

      "must redirect to a knockback page when a BTN is submitted" in {
        when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(submittedBTNRecord)))

        val application = applicationBuilder(userAnswers = Some(submittedBTNRecord), subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          val request = FakeRequest(GET, CheckYourAnswersController.onPageLoad.url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual CheckYourAnswersController.cannotReturnKnockback.url
        }
      }

      "must redirect to waiting room when a submission is processing" in {
        val processingUa = validBTNCyaUa.set(BTNStatus, BTNStatus.processing).get

        when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(processingUa)))

        val application = applicationBuilder(userAnswers = Some(processingUa), subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          val request = FakeRequest(GET, CheckYourAnswersController.onPageLoad.url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual BTNWaitingRoomController.onPageLoad.url
        }
      }

      "must redirect to JourneyRecoveryController on retrieval of answers failure" in {
        val application = applicationBuilder(userAnswers = None, subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          val request = FakeRequest(GET, CheckYourAnswersController.onPageLoad.url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual "/report-pillar2-submission-top-up-taxes/there-is-a-problem"
        }
      }
    }

    ".onSubmit" should {

      "must immediately redirect to the waiting room when submission starts" in {

        val slowPromise = Promise[BTNSuccess]()
        val slowFuture  = slowPromise.future

        when(mockBTNService.submitBTN(any())(any(), any())).thenReturn(slowFuture)
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(validBTNCyaUa), subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[BTNService].toInstance(mockBTNService))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          val request = FakeRequest(POST, CheckYourAnswersController.onSubmit.url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual BTNWaitingRoomController.onPageLoad.url

          verify(mockSessionRepository).set(any())
        }
      }

      "must update the status after a successful API call completes" in {

        val successPromise = Promise[BTNSuccess]()
        val successFuture  = successPromise.future

        when(mockBTNService.submitBTN(any())(any(), any())).thenReturn(successFuture)
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockAuditService.auditBTN(any(), any(), any(), any())(any())).thenReturn(Future.successful(AuditResult.Success))

        val application = applicationBuilder(userAnswers = Some(validBTNCyaUa), subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[BTNService].toInstance(mockBTNService))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .build()

        running(application) {
          val request = FakeRequest(POST, CheckYourAnswersController.onSubmit.url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual BTNWaitingRoomController.onPageLoad.url

          verify(mockSessionRepository).set(any())

          successPromise.success(BTNSuccess(ZonedDateTime.now()))
        }
      }

      "must update the status after a failed API call" in {

        val failPromise = Promise[BTNSuccess]()
        val failFuture  = failPromise.future

        when(mockBTNService.submitBTN(any())(any(), any())).thenReturn(failFuture)
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockAuditService.auditBTN(any(), any(), any(), any())(any())).thenReturn(Future.successful(AuditResult.Success))

        val application = applicationBuilder(userAnswers = Some(validBTNCyaUa), subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[BTNService].toInstance(mockBTNService))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .build()

        running(application) {
          val request = FakeRequest(POST, CheckYourAnswersController.onSubmit.url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual BTNWaitingRoomController.onPageLoad.url

          verify(mockSessionRepository).set(any())

          failPromise.failure(InternalIssueError)
        }
      }

      "must redirect to waiting room when BTN submission throws an exception" in {
        when(mockBTNService.submitBTN(any())(any(), any())).thenReturn(Future.failed(InternalIssueError))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(validBTNCyaUa), subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[BTNService].toInstance(mockBTNService))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          val request = FakeRequest(POST, CheckYourAnswersController.onSubmit.url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual BTNWaitingRoomController.onPageLoad.url
        }
      }

      "must redirect to waiting room when BTN submission returns Future.failed(ApiError)" in {
        when(mockBTNService.submitBTN(any())(any(), any())).thenReturn(Future.failed(InternalIssueError))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(validBTNCyaUa), subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[BTNService].toInstance(mockBTNService))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          val request = FakeRequest(POST, CheckYourAnswersController.onSubmit.url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual BTNWaitingRoomController.onPageLoad.url
        }
      }

      "must redirect to waiting room for any other error" in {
        when(mockBTNService.submitBTN(any())(any(), any())).thenReturn(Future.failed(new RuntimeException("Some other error")))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(validBTNCyaUa), subscriptionLocalData = Some(someSubscriptionLocalData))
          .overrides(bind[BTNService].toInstance(mockBTNService))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

        running(application) {
          val request = FakeRequest(POST, CheckYourAnswersController.onSubmit.url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual BTNWaitingRoomController.onPageLoad.url
        }
      }
    }

    ".cannotReturnKnockback" should {
      "return BAD_REQUEST and render the knockback view" in {
        val application = applicationBuilder(userAnswers = Some(validBTNCyaUa), subscriptionLocalData = Some(someSubscriptionLocalData))
          .build()

        running(application) {
          val request = FakeRequest(GET, CheckYourAnswersController.cannotReturnKnockback.url)
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
        }
      }
    }
  }
}
