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

import models.obligationsandsubmissions.{AccountingPeriodDetails, ObligationStatus, ObligationType, Submission}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.{HeadCell, Table, TableRow}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object SubmissionHistoryHelper {

  def generateSubmissionHistoryTable(accountingPeriods: Seq[AccountingPeriodDetails])(implicit
    messages:                                           Messages
  ): Seq[Table] =
    accountingPeriods
      .filter(accountPeriod => hasDisplayableObligations(accountPeriod))
      .map { periodsWithSubmissions =>
        val rows = createRowsForAccountingPeriod(periodsWithSubmissions)
        createTable(periodsWithSubmissions.startDate, periodsWithSubmissions.endDate, rows)
      }

  private def hasDisplayableObligations(accountPeriod: AccountingPeriodDetails): Boolean =
    accountPeriod.obligations.exists { obligation =>
      // Show if has submissions OR if it's a fulfilled GIR obligation (indicating BTN submission)
      obligation.submissions.nonEmpty ||
      (obligation.obligationType == ObligationType.GIR && obligation.status == ObligationStatus.Fulfilled)
    }

  private def createRowsForAccountingPeriod(accountingPeriod: AccountingPeriodDetails)(implicit messages: Messages): Seq[Seq[TableRow]] =
    accountingPeriod.obligations.flatMap { obligation =>
      if (obligation.submissions.nonEmpty) {
        // Standard case: show all submissions
        obligation.submissions.map(createTableRows)
      } else if (obligation.obligationType == ObligationType.GIR && obligation.status == ObligationStatus.Fulfilled) {
        // Special case: GIR obligation fulfilled by BTN (no submissions)
        Seq(createGIRFulfilledByBTNRows())
      } else {
        // No rows for other obligations without submissions
        Seq.empty
      }
    }

  def createTable(startDate: LocalDate, endDate: LocalDate, rows: Seq[Seq[TableRow]])(implicit messages: Messages): Table = {
    val formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
    val formattedEndDate   = endDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

    Table(
      caption = Some(s"$formattedStartDate to $formattedEndDate"),
      rows = rows,
      head = Some(
        Seq(
          HeadCell(
            Text(messages("submissionHistory.typeOfReturn")),
            classes = "govuk-table__header govuk-!-width-two-thirds",
            attributes = Map("scope" -> "col")
          ),
          HeadCell(
            Text(messages("submissionHistory.submissionDate")),
            classes = "govuk-table__header govuk-!-width-two-thirds",
            attributes = Map("scope" -> "col")
          )
        )
      )
    )
  }

  def createTableRows(submission: Submission): Seq[TableRow] =
    Seq(
      TableRow(
        content = Text(submission.submissionType.fullName)
      ),
      TableRow(
        content = Text(submission.receivedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy")))
      )
    )

  private def createGIRFulfilledByBTNRows()(implicit messages: Messages): Seq[TableRow] =
    Seq(
      TableRow(
        content = Text(messages("submissionHistory.girFulfilledByBTN.returnType"))
      ),
      TableRow(
        content = Text(messages("submissionHistory.girFulfilledByBTN.status"))
      )
    )

}
