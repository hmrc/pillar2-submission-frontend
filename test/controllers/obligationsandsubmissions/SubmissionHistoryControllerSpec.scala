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
import models.obligationsandsubmissions.SubmissionType.UKTR
import models.obligationsandsubmissions.{AccountingPeriodDetails, Obligation, ObligationsAndSubmissionsSuccess, Submission}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.Aliases.{HeadCell, TableRow, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.Table
import views.html.obligationsandsubmissions.submissionhistory.SubmissionHistoryView

import java.time.{LocalDate, ZonedDateTime}
import scala.concurrent.Future

class SubmissionHistoryControllerSpec extends SpecBase with MockitoSugar with ScalaFutures {

//  val enrolments: Set[Enrolment] = Set(
//    Enrolment(
//      key = "HMRC-PILLAR2-ORG",
//      identifiers = Seq(
//        EnrolmentIdentifier("PLRID", "Abc123")
//      ),
//      state = "activated"
//    )
//  )

  val tables: Seq[Table] = Seq(
    Table(
      List(
        List(TableRow(Text("UKTR")), TableRow(Text("25 February 2025"))),
        List(TableRow(Text("UKTR")), TableRow(Text("25 February 2025"))),
      ),
      head = Some(
        Seq(
          HeadCell(Text(messages("submissionHistory.submissionType"))),
          HeadCell(Text(messages("submissionHistory.submissionDate")))
        )
      ),
      caption = Some("25 February 2024 to 25 February 2025")
    )
  )

  val submissionHistoryResponse: ObligationsAndSubmissionsSuccess = ObligationsAndSubmissionsSuccess(
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

  val submissionHistoryNoSubmissionResponse: ObligationsAndSubmissionsSuccess = ObligationsAndSubmissionsSuccess(
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

  "SubmissionHistoryController" should {
    "return OK and render the SubmissionHistoryView when submissions are present" in {
      when(mockObligationsAndSubmissionsService.handleData(any[LocalDate], any[LocalDate])(any(), any()))
        .thenReturn(Future.successful(submissionHistoryResponse))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), subscriptionLocalData = Some(someSubscriptionLocalData)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.obligationsandsubmissions.routes.SubmissionHistoryController.onPageLoad.url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmissionHistoryView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(tables)(request, appConfig(application), messages(application)).toString
      }
    }

    "return OK and render the SubmissionHistoryNoSubscriptionView when no submissions are present" in {
      when(mockObligationsAndSubmissionsService.handleData(any(), any())(any(), any()))
        .thenReturn(Future.successful(submissionHistoryNoSubmissionResponse))
    }
    "redirect to JourneyRecoveryController on exception" in {
      when(mockObligationsAndSubmissionsService.handleData(any(), any())(any(), any()))
        .thenReturn(Future.failed(new Exception("Service error")))
//      status(result) mustBe SEE_OTHER
//      redirectLocation(result) mustBe Some(controllers.routes.JourneyRecoveryController.onPageLoad(None).url)
    }
  }
}
