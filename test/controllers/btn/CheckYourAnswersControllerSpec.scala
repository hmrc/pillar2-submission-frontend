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
import models.audit.ApiResponseData
import models.btn.BTNSuccess
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
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
import services.audit.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import viewmodels.govuk.SummaryListFluency
import views.html.btn.CheckYourAnswersView

import java.time.ZonedDateTime
import scala.concurrent.Future

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
        redirectLocation(result).value mustEqual controllers.btn.routes.BTNProblemWithServiceController.onPageLoad.url
      }
    }

    ".onSubmit" should {

      def submitBTNResult(serviceResponse: Future[BTNSuccess]): Future[Result] = {
        when(mockBTNService.submitBTN(any())(any(), any())).thenReturn(serviceResponse)
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockAuditService.auditBTN(any(), any(), any(), any())(any())).thenReturn(Future.successful(AuditResult.Success))
        when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(validBTNCyaUa)))

        val request = FakeRequest(POST, CheckYourAnswersController.onSubmit.url)
        route(application, request).value
      }

      def submitFailingBTNResult(error: Throwable): Future[Result] = {
        when(mockBTNService.submitBTN(any())(any(), any())).thenReturn(Future.failed(error))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
        when(mockAuditService.auditBTN(any(), any(), any(), any())(any())).thenReturn(Future.successful(AuditResult.Success))
        when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(validBTNCyaUa)))

        val request = FakeRequest(POST, CheckYourAnswersController.onSubmit.url)
        route(application, request).value
      }

      "must redirect to Confirmation page on submission" in {
        val successResponse = BTNSuccess(ZonedDateTime.now())
        val result          = submitBTNResult(Future.successful(successResponse))

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual BTNConfirmationController.onPageLoad.url

        verify(mockAuditService).auditBTN(
          eqTo("Abc123"),
          eqTo("AccountingPeriod(2024-10-24,2025-10-24,None)"),
          eqTo(false),
          any[ApiResponseData]
        )(any[HeaderCarrier])
      }

      "redirect to Problem with Service page when BTN submission throws an exception" in {
        val result = submitFailingBTNResult(InternalIssueError)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual BTNProblemWithServiceController.onPageLoad.url

        verify(mockAuditService).auditBTN(
          eqTo("Abc123"),
          eqTo("AccountingPeriod(2024-10-24,2025-10-24,None)"),
          eqTo(false),
          any[ApiResponseData]
        )(any[HeaderCarrier])
      }

      "redirect to Problem with Service page when BTN submission returns Future.failed(ApiError)" in {
        val result = submitFailingBTNResult(InternalIssueError)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual BTNProblemWithServiceController.onPageLoad.url

        verify(mockAuditService).auditBTN(
          eqTo("Abc123"),
          eqTo("AccountingPeriod(2024-10-24,2025-10-24,None)"),
          eqTo(false),
          any[ApiResponseData]
        )(any[HeaderCarrier])
      }

      "redirect to Problem with Service page for any other error" in {
        val result = submitFailingBTNResult(InternalIssueError)

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual BTNProblemWithServiceController.onPageLoad.url

        verify(mockAuditService).auditBTN(
          eqTo("Abc123"),
          eqTo("AccountingPeriod(2024-10-24,2025-10-24,None)"),
          eqTo(false),
          any[ApiResponseData]
        )(any[HeaderCarrier])
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
