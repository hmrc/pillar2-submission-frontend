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
import models.obligationsandsubmissions.ObligationStatus.Fulfilled
import models.obligationsandsubmissions.ObligationType.Pillar2TaxReturn
import models.obligationsandsubmissions.SubmissionType.{GIR, UKTR}
import models.obligationsandsubmissions._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.obligationsandsubmissions.ObligationsAndSubmissionsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.obligationsandsubmissions.submissionhistory.{SubmissionHistoryNoSubmissionsView, SubmissionHistoryView}

import java.time.{LocalDate, ZonedDateTime}
import scala.concurrent.Future

class SubmissionHistoryControllerSpec extends SpecBase with MockitoSugar with ScalaFutures {

  val submissionHistoryResponse: ObligationsAndSubmissionsSuccess = ObligationsAndSubmissionsSuccess(
    ZonedDateTime.now,
    Seq(
      AccountingPeriodDetails(
        LocalDate.now.minusDays(1).minusYears(7),
        LocalDate.now,
        LocalDate.now,
        underEnquiry = false,
        Seq(
          Obligation(
            Pillar2TaxReturn,
            Fulfilled,
            canAmend = true,
            Seq(
              Submission(
                UKTR,
                ZonedDateTime.now,
                None
              ),
              Submission(
                GIR,
                ZonedDateTime.now,
                None
              )
            )
          )
        )
      ),
      AccountingPeriodDetails(
        LocalDate.now.minusDays(1).minusYears(7),
        LocalDate.now,
        LocalDate.now,
        underEnquiry = false,
        Seq(
          Obligation(
            Pillar2TaxReturn,
            Fulfilled,
            canAmend = true,
            Seq(
              Submission(
                UKTR,
                ZonedDateTime.now,
                None
              )
            )
          )
        )
      )
    )
  )

  val noSubmissionHistoryResponse: ObligationsAndSubmissionsSuccess = ObligationsAndSubmissionsSuccess(
    ZonedDateTime.now,
    Seq(
      AccountingPeriodDetails(
        LocalDate.now,
        LocalDate.now,
        LocalDate.now,
        underEnquiry = false,
        Seq(
          Obligation(
            Pillar2TaxReturn,
            Fulfilled,
            canAmend = true,
            Seq.empty
          )
        )
      )
    )
  )

  "SubmissionHistoryController" must {
    "return OK and render the SubmissionHistoryView when submissions are present" in {
      when(mockObligationsAndSubmissionsService.handleData(any[LocalDate], any[LocalDate])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(submissionHistoryResponse))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
        .overrides(
          bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.obligationsandsubmissions.routes.SubmissionHistoryController.onPageLoad.url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmissionHistoryView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(submissionHistoryResponse.accountingPeriodDetails)(
          request,
          appConfig(application),
          messages(application)
        ).toString
      }
    }

    "return OK and render the SubmissionHistoryNoSubscriptionView when no submissions are present" in {
      when(mockObligationsAndSubmissionsService.handleData(any[LocalDate], any[LocalDate])(any[HeaderCarrier], any[String]))
        .thenReturn(Future.successful(noSubmissionHistoryResponse))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
        .overrides(
          bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.obligationsandsubmissions.routes.SubmissionHistoryController.onPageLoad.url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmissionHistoryNoSubmissionsView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view()(request, appConfig(application), messages(application)).toString
      }
    }

    "redirect to JourneyRecoveryController on exception" in {
      when(mockObligationsAndSubmissionsService.handleData(any(), any())(any(), any()))
        .thenReturn(Future.failed(new Exception("something went wrong")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), subscriptionLocalData = Some(someSubscriptionLocalData))
        .overrides(
          bind[ObligationsAndSubmissionsService].toInstance(mockObligationsAndSubmissionsService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.obligationsandsubmissions.routes.SubmissionHistoryController.onPageLoad.url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.JourneyRecoveryController.onPageLoad(None).url)
      }
    }
  }
}
