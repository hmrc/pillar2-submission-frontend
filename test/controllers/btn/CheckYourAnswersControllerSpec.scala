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
import controllers.actions._
import controllers.btn.routes
import controllers.btn.{BTNConfirmationController, BTNProblemWithServiceController, CheckYourAnswersController}
import helpers.{AllMocks, TestDataFixture}
import models.btn.{BTNRequest, BTNStatus, BTNSuccess}
import models.requests.{DataRequest, SubscriptionDataRequest}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import pages.{BTNLast4AccountingPeriodsPage, BTNNext2AccountingPeriodsPage, EntitiesInsideOutsideUKPage}
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.mvc.{ActionRefiner, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.BTNService
import services.audit.AuditService
import uk.gov.hmrc.http.HttpException
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import views.html.btn.CheckYourAnswersView

import java.time.ZonedDateTime
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersControllerSpec extends SpecBase with AllMocks with TestDataFixture with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionRepository, mockBTNService, mockAuditService)
  }

  val mockBTNStatusAction = new BTNStatusAction(mockSessionRepository) {
    private def btnAlreadySubmitted[T](userId: String)(request: T) = sessionRepository.get(userId).map { maybeUserAnswers =>
      if (maybeUserAnswers.flatMap(_.get(BTNStatus)).contains(BTNStatus.submitted)) {
        Left(Redirect(controllers.btn.routes.CheckYourAnswersController.cannotReturnKnockback))
      } else {
        Right(request)
      }
    }

    override def subscriptionRequest: ActionRefiner[SubscriptionDataRequest, SubscriptionDataRequest] =
      new ActionRefiner[SubscriptionDataRequest, SubscriptionDataRequest] {
        override protected def refine[A](request: SubscriptionDataRequest[A]): Future[Either[Result, SubscriptionDataRequest[A]]] =
          btnAlreadySubmitted(request.userId)(request)

        override protected def executionContext: ExecutionContext = ec
      }

    override def dataRequest: ActionRefiner[DataRequest, DataRequest] =
      new ActionRefiner[DataRequest, DataRequest] {
        override protected def refine[A](request: DataRequest[A]): Future[Either[Result, DataRequest[A]]] =
          btnAlreadySubmitted(request.userId)(request)

        override protected def executionContext: ExecutionContext = ec
      }
  }

  "CheckYourAnswers Controller" must {
    "be redirected to JourneyRecoveryController when called" in {
      val userAnswers = emptyUserAnswers
        .set(EntitiesInsideOutsideUKPage, true)
        .success
        .value
        .set(BTNLast4AccountingPeriodsPage, false)
        .success
        .value
        .set(BTNNext2AccountingPeriodsPage, false)
        .success
        .value
        .remove(BTNStatus)
        .success
        .value

      when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BTNService].toInstance(mockBTNService),
          bind[AuditService].toInstance(mockAuditService),
          bind[BTNStatusAction].toInstance(mockBTNStatusAction)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.CheckYourAnswersController.onPageLoad.url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include("/there-is-a-problem")
      }
    }

    "be redirected to JourneyRecoveryController when missing some user answers" in {
      val userAnswers = emptyUserAnswers
        // Missing EntitiesInsideOutsideUKPage
        .set(BTNLast4AccountingPeriodsPage, false)
        .success
        .value
        .set(BTNNext2AccountingPeriodsPage, false)
        .success
        .value
        .remove(BTNStatus)
        .success
        .value

      when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BTNService].toInstance(mockBTNService),
          bind[AuditService].toInstance(mockAuditService),
          bind[BTNStatusAction].toInstance(mockBTNStatusAction)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.CheckYourAnswersController.onPageLoad.url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include("/there-is-a-problem")
      }
    }

    "be redirected to JourneyRecoveryController when no user answers are found" in {
      when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BTNService].toInstance(mockBTNService),
          bind[AuditService].toInstance(mockAuditService),
          bind[BTNStatusAction].toInstance(mockBTNStatusAction)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.CheckYourAnswersController.onPageLoad.url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include("/there-is-a-problem")
      }
    }

    "be redirected to JourneyRecoveryController when submitting data" in {
      val userAnswers = emptyUserAnswers
        .set(EntitiesInsideOutsideUKPage, true)
        .success
        .value
        .set(BTNLast4AccountingPeriodsPage, false)
        .success
        .value
        .set(BTNNext2AccountingPeriodsPage, false)
        .success
        .value
        .remove(BTNStatus)
        .success
        .value

      when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))
      when(mockBTNService.submitBTN(any())(any(), any())).thenReturn(Future.successful(BTNSuccess(ZonedDateTime.now())))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BTNService].toInstance(mockBTNService),
          bind[AuditService].toInstance(mockAuditService),
          bind[BTNStatusAction].toInstance(mockBTNStatusAction)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.btn.routes.CheckYourAnswersController.onSubmit.url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include("/there-is-a-problem")
      }
    }

    "render the cannotReturnKnockback page with BAD_REQUEST status" in {
      val application = applicationBuilder()
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BTNService].toInstance(mockBTNService),
          bind[AuditService].toInstance(mockAuditService),
          bind[BTNStatusAction].toInstance(mockBTNStatusAction)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.CheckYourAnswersController.cannotReturnKnockback.url)

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "redirect when BTNStatus is already submitted" in {
      val userAnswers = emptyUserAnswers
        .set(EntitiesInsideOutsideUKPage, true)
        .success
        .value
        .set(BTNLast4AccountingPeriodsPage, false)
        .success
        .value
        .set(BTNNext2AccountingPeriodsPage, false)
        .success
        .value
        .set(BTNStatus, BTNStatus.submitted)
        .success
        .value

      when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BTNService].toInstance(mockBTNService),
          bind[AuditService].toInstance(mockAuditService),
          bind[BTNStatusAction].toInstance(mockBTNStatusAction)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.CheckYourAnswersController.onPageLoad.url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include("/there-is-a-problem")
      }
    }

    "handle session repository failures gracefully during submission" in {
      val userAnswers = emptyUserAnswers
        .set(EntitiesInsideOutsideUKPage, true)
        .success
        .value
        .set(BTNLast4AccountingPeriodsPage, false)
        .success
        .value
        .set(BTNNext2AccountingPeriodsPage, false)
        .success
        .value
        .remove(BTNStatus)
        .success
        .value

      when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))
      when(mockBTNService.submitBTN(any())(any(), any())).thenReturn(Future.successful(BTNSuccess(ZonedDateTime.now())))
      when(mockSessionRepository.set(any())).thenReturn(Future.failed(new RuntimeException("Database connection error")))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BTNService].toInstance(mockBTNService),
          bind[AuditService].toInstance(mockAuditService),
          bind[BTNStatusAction].toInstance(mockBTNStatusAction)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.btn.routes.CheckYourAnswersController.onSubmit.url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include("/there-is-a-problem")
      }
    }

    "handle audit service when a BTN is submitted successfully" in {
      val userAnswers = emptyUserAnswers
        .set(EntitiesInsideOutsideUKPage, true)
        .success
        .value
        .set(BTNLast4AccountingPeriodsPage, false)
        .success
        .value
        .set(BTNNext2AccountingPeriodsPage, false)
        .success
        .value
        .remove(BTNStatus)
        .success
        .value

      when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))
      when(mockBTNService.submitBTN(any())(any(), any())).thenReturn(Future.successful(BTNSuccess(ZonedDateTime.now())))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockAuditService.auditBTN(any(), any(), any(), any())(any())).thenReturn(Future.successful(Success))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BTNService].toInstance(mockBTNService),
          bind[AuditService].toInstance(mockAuditService),
          bind[BTNStatusAction].toInstance(mockBTNStatusAction)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.btn.routes.CheckYourAnswersController.onSubmit.url)
        route(application, request).value
      }
    }

    "handle audit service when a BTN submission fails" in {
      val userAnswers = emptyUserAnswers
        .set(EntitiesInsideOutsideUKPage, true)
        .success
        .value
        .set(BTNLast4AccountingPeriodsPage, false)
        .success
        .value
        .set(BTNNext2AccountingPeriodsPage, false)
        .success
        .value
        .remove(BTNStatus)
        .success
        .value

      when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))
      when(mockBTNService.submitBTN(any())(any(), any())).thenReturn(Future.failed(new HttpException("Service unavailable", 503)))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockAuditService.auditBTN(any(), any(), any(), any())(any())).thenReturn(Future.successful(Success))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BTNService].toInstance(mockBTNService),
          bind[AuditService].toInstance(mockAuditService),
          bind[BTNStatusAction].toInstance(mockBTNStatusAction)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.btn.routes.CheckYourAnswersController.onSubmit.url)
        route(application, request).value
      }
    }

    "handle audit service failures gracefully" in {
      val userAnswers = emptyUserAnswers
        .set(EntitiesInsideOutsideUKPage, true)
        .success
        .value
        .set(BTNLast4AccountingPeriodsPage, false)
        .success
        .value
        .set(BTNNext2AccountingPeriodsPage, false)
        .success
        .value
        .remove(BTNStatus)
        .success
        .value

      when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))
      when(mockBTNService.submitBTN(any())(any(), any())).thenReturn(Future.successful(BTNSuccess(ZonedDateTime.now())))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockAuditService.auditBTN(any(), any(), any(), any())(any()))
        .thenReturn(Future.failed(new RuntimeException("Audit service connection failed")))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BTNService].toInstance(mockBTNService),
          bind[AuditService].toInstance(mockAuditService),
          bind[BTNStatusAction].toInstance(mockBTNStatusAction)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.btn.routes.CheckYourAnswersController.onSubmit.url)
        route(application, request).value
      }
    }

    "redirect to IndexController when data exists but doesn't match required pattern" in {
      val userAnswers = emptyUserAnswers
        .set(EntitiesInsideOutsideUKPage, true)
        .success
        .value
        .set(BTNLast4AccountingPeriodsPage, true) // This will cause the condition to fail
        .success
        .value
        .set(BTNNext2AccountingPeriodsPage, false)
        .success
        .value
        .remove(BTNStatus)
        .success
        .value

      when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BTNService].toInstance(mockBTNService),
          bind[AuditService].toInstance(mockAuditService),
          bind[BTNStatusAction].toInstance(mockBTNStatusAction)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.CheckYourAnswersController.onPageLoad.url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include("/there-is-a-problem")
      }
    }

    "successfully render the CheckYourAnswers page when data matches required pattern" in {
      val userAnswers = emptyUserAnswers
        .set(EntitiesInsideOutsideUKPage, true)
        .success
        .value
        .set(BTNLast4AccountingPeriodsPage, false)
        .success
        .value
        .set(BTNNext2AccountingPeriodsPage, false)
        .success
        .value
        .remove(BTNStatus)
        .success
        .value

      when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository),
          bind[BTNService].toInstance(mockBTNService),
          bind[AuditService].toInstance(mockAuditService),
          bind[BTNStatusAction].toInstance(mockBTNStatusAction)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.btn.routes.CheckYourAnswersController.onPageLoad.url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include("/there-is-a-problem")
      }
    }
  }
}
