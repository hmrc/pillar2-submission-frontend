/*
 * Copyright 2025 HM Revenue & Customs
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

package views.helpers

import models.obligationsandsubmissions.ObligationStatus.{Fulfilled, Open}
import models.obligationsandsubmissions.ObligationType.{GlobeInformationReturn, Pillar2TaxReturn}
import models.obligationsandsubmissions.SubmissionType.{GIR, UKTR}
import models.obligationsandsubmissions.{AccountingPeriodDetails, Obligation, Submission}
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}

class SubmissionHistoryHelperSpec extends AnyWordSpec with Matchers with MockitoSugar {
  implicit val messages: Messages = mock[Messages]

  "SubmissionHistoryHelper" must {
    "generate a table for each accounting period" in {
      val startDate1 = LocalDate.of(2024, 1, 1)
      val endDate1   = LocalDate.of(2024, 12, 31)
      val startDate2 = LocalDate.of(2023, 1, 1)
      val endDate2   = LocalDate.of(2023, 12, 31)

      val submission1 = Submission(UKTR, ZonedDateTime.now, None)
      val submission2 = Submission(GIR, ZonedDateTime.now, None)

      val obligation1 = Obligation(Pillar2TaxReturn, Open, canAmend = true, Seq(submission1, submission2))
      val obligation2 = Obligation(GlobeInformationReturn, Fulfilled, canAmend = false, Seq(submission1))

      val accountingPeriods = Seq(
        AccountingPeriodDetails(startDate1, endDate1, LocalDate.now, underEnquiry = false, Seq(obligation1)),
        AccountingPeriodDetails(startDate2, endDate2, LocalDate.now, underEnquiry = false, Seq(obligation2))
      )

      when(messages("submissionHistory.typeOfReturn")).thenReturn("Type of return")
      when(messages("submissionHistory.submissionDate")).thenReturn("Submission Date")

      val tables = SubmissionHistoryHelper.generateSubmissionHistoryTable(accountingPeriods)

      tables                   should have length 2
      tables.head.caption    shouldBe Some("01 January 2024 to 31 December 2024")
      tables.last.caption    shouldBe Some("01 January 2023 to 31 December 2023")
      tables.head.rows.flatten should have length 4
      tables.last.rows.flatten should have length 2
    }

    "format table caption correctly" in {
      val startDate = LocalDate.of(2024, 1, 1)
      val endDate   = LocalDate.of(2024, 12, 31)
      val table     = SubmissionHistoryHelper.createTable(startDate, endDate, Seq.empty)
      table.caption shouldBe Some("01 January 2024 to 31 December 2024")
    }

    "create table rows correctly" in {
      val submissionDate = ZonedDateTime.now.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))

      val submission = Submission(UKTR, ZonedDateTime.now, None)
      val rows       = SubmissionHistoryHelper.createTableRows(submission)
      rows                should have length 2
      rows.head.content shouldBe Text("UK Tax Return")
      rows.last.content shouldBe Text(submissionDate)
    }
  }
}
