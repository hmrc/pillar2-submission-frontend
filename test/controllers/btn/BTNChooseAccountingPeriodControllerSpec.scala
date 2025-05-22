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
import connectors.SubscriptionConnector
import forms.BTNChooseAccountingPeriodFormProvider
import models.obligationsandsubmissions.ObligationStatus.Open
import models.obligationsandsubmissions.ObligationType.UKTR
import models.obligationsandsubmissions._
import models.{Mode, NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.BTNChooseAccountingPeriodPage
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.obligationsandsubmissions.ObligationsAndSubmissionsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.btn.BTNChooseAccountingPeriodView

import java.time.{LocalDate, ZonedDateTime}
import scala.concurrent.Future

class BTNChooseAccountingPeriodControllerSpec extends SpecBase {
  val mode: Mode = NormalMode
  val formProvider = new BTNChooseAccountingPeriodFormProvider()
  val form: Form[Int] = formProvider()

  val plrReference     = "testPlrRef"
  val organisationName = "orgName"
  val obligationData: Seq[Obligation] = Seq(Obligation(UKTR, Open, canAmend = false, Seq.empty))

  val obligationsAndSubmissionsSuccess: ObligationsAndSubmissionsSuccess =
    ObligationsAndSubmissionsSuccess(
      ZonedDateTime.now,
      Seq(
        AccountingPeriodDetails(
          LocalDate.now.minusYears(3),
          LocalDate.now.minusYears(2),
          LocalDate.now,
          underEnquiry = false,
          obligationData
        ),
        AccountingPeriodDetails(
          LocalDate.now.minusYears(2),
          LocalDate.now.minusYears(1),
          LocalDate.now.plusYears(1),
          underEnquiry = false,
          obligationData
        ),
        AccountingPeriodDetails(
          LocalDate.now.minusYears(1),
          LocalDate.now,
          LocalDate.now.plusYears(2),
          underEnquiry = false,
          obligationData
        )
      )
    )

  val zippedAccountingPeriodDetails: Seq[(AccountingPeriodDetails, Int)] = obligationsAndSubmissionsSuccess.accountingPeriodDetails.zipWithIndex
  val chosenAccountingPeriod:        (AccountingPeriodDetails, Int)      = zippedAccountingPeriodDetails.head

  def application: Application = applicationBuilder(subscriptionLocalData = Some(emptySubscriptionLocalData), userAnswers = Some(emptyUserAnswers))
    .overrides(
      bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
    )
    .build()

  "BTNChooseAccountingPeriodController" must {
    "must return OK and the correct view for a GET" in {
      running(application) {
        when(mockObligationsAndSubmissionsService.handleData(any[String], any[LocalDate], any[LocalDate])(any[HeaderCarrier]))
          .thenReturn(Future.successful(obligationsAndSubmissionsSuccess))

        val request = FakeRequest(GET, controllers.btn.routes.BTNChooseAccountingPeriodController.onPageLoad(mode).url)
        val result  = route(application, request).value

        val view = application.injector.instanceOf[BTNChooseAccountingPeriodView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, mode, isAgent = false, organisationName, zippedAccountingPeriodDetails)(
          request,
          appConfig(application),
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(BTNChooseAccountingPeriodPage, chosenAccountingPeriod._1).success.value

      def application: Application = applicationBuilder(subscriptionLocalData = Some(emptySubscriptionLocalData), userAnswers = Some(userAnswers))
        .overrides(
          bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
        )
        .build()

      running(application) {
        when(mockObligationsAndSubmissionsService.handleData(any[String], any[LocalDate], any[LocalDate])(any[HeaderCarrier]))
          .thenReturn(Future.successful(obligationsAndSubmissionsSuccess))

        val request = FakeRequest(GET, controllers.btn.routes.BTNChooseAccountingPeriodController.onPageLoad(mode).url)

        val view = application.injector.instanceOf[BTNChooseAccountingPeriodView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(chosenAccountingPeriod._2),
          mode,
          isAgent = false,
          organisationName,
          zippedAccountingPeriodDetails
        )(
          request,
          appConfig(application),
          messages(application)
        ).toString
      }
    }

    "must redirect to the AccountingPeriod page when a valid answer is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      running(application) {
        when(mockObligationsAndSubmissionsService.handleData(any[String], any[LocalDate], any[LocalDate])(any[HeaderCarrier]))
          .thenReturn(Future.successful(obligationsAndSubmissionsSuccess))

        val request =
          FakeRequest(POST, controllers.btn.routes.BTNChooseAccountingPeriodController.onPageLoad(mode).url)
            .withFormUrlEncodedBody(("value", "1"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.btn.routes.BTNAccountingPeriodController.onPageLoad(mode).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      running(application) {
        val request =
          FakeRequest(POST, controllers.btn.routes.BTNChooseAccountingPeriodController.onPageLoad(mode).url)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[BTNChooseAccountingPeriodView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, mode, isAgent = false, organisationName, zippedAccountingPeriodDetails)(
          request,
          appConfig(application),
          messages(application)
        ).toString
      }
    }

    "redirect to BTN error page when no subscription data is found" in {
      def application: Application = applicationBuilder(subscriptionLocalData = None, userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
        )
        .build()

      running(application) {
        when(mockObligationsAndSubmissionsService.handleData(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(obligationsAndSubmissionsSuccessResponse(ObligationStatus.Open)))

        val request = FakeRequest(GET, controllers.btn.routes.BTNChooseAccountingPeriodController.onPageLoad(mode).url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.btn.routes.BTNProblemWithServiceController.onPageLoad.url
      }
    }

    "redirect to BTN error page if obligations service fails" in {
      val application = applicationBuilder(subscriptionLocalData = Some(emptySubscriptionLocalData), userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
          bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
        )
        .build()

      running(application) {
        when(mockObligationsAndSubmissionsService.handleData(any[String], any[LocalDate], any[LocalDate])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new Exception("Service failed")))

        val request = FakeRequest(GET, controllers.btn.routes.BTNChooseAccountingPeriodController.onPageLoad(mode).url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.btn.routes.BTNProblemWithServiceController.onPageLoad.url
      }
    }
  }
}
