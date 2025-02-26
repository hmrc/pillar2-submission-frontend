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

package views.obligationsandsubmissions.submissionhistory

import base.ViewSpecBase
import models.obligationsandsubmissions.ObligationStatus.Fulfilled
import models.obligationsandsubmissions.ObligationType.Pillar2TaxReturn
import models.obligationsandsubmissions.SubmissionType.UKTR
import models.obligationsandsubmissions._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.html.obligationsandsubmissions.submissionhistory.SubmissionHistoryView

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}

class SubmissionHistoryViewSpec extends ViewSpecBase {

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

  val page: SubmissionHistoryView = inject[SubmissionHistoryView]

  val view: Document = Jsoup.parse(page(submissionHistoryResponse.accountingPeriodDetails)(request, appConfig, messages).toString())

  "Transaction History View" should {

    "have a title" in {
      val title = "Submission history - Report Pillar 2 top-up taxes - GOV.UK"
      view.getElementsByTag("title").text must include(title)
    }

    "have a heading" in {
      view.getElementsByTag("h1").text must include("Submission history")
    }

    "have a first paragraph" in {
      view.getElementsByTag("p").text must include(
        "You can find all submissions and amendments made by your group during this accounting period and the previous 6 accounting periods."
      )
    }

    "have a second paragraph" in {
      view.getElementsByTag("p").text must include(
        "Where you’ve made changes to a tax return or information return, we’ll list these as individual submissions."
      )
    }

    "have a inset text" in {
      view.getElementsByClass("govuk-inset-text").text must include(
        "You can amend your submissions at any time, except for the UK Tax Return, which must be updated within 12 months of the submission deadline."
      )
    }

    "have a table" in {
      val fromDate:       String = LocalDate.now.minusDays(1).minusYears(7).format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
      val toDate:         String = LocalDate.now.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
      val submissionDate: String = ZonedDateTime.now.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))

      val captions = view.getElementsByClass("govuk-table__caption")
      captions.first().text must include(s"$fromDate to $toDate")

      val tableHeaders = view.getElementsByClass("govuk-table__header")

      tableHeaders.first().text must include("Submission type")
      tableHeaders.get(1).text  must include("Submission date")

      (1 to 2).foreach { int =>
        val tableRow = view.getElementsByClass("govuk-table__row").get(int).getElementsByClass("govuk-table__cell")
        tableRow.first().text must include("UKTR")
        tableRow.get(1).text  must include(submissionDate)
      }
    }

    "have a sub heading" in {
      view.getElementsByTag("h2").text must include("Due and overdue returns")
    }

    "have a paragraph with link" in {
      val link = view.getElementsByClass("govuk-body").last().getElementsByTag("a")
      view.getElementsByTag("p").text must include(
        "Information on your group’s"
      )
      link.text must include("due and overdue returns")
      link.attr("href") mustEqual "#" //TODO: Change to URL when returns page built
    }
  }
}
