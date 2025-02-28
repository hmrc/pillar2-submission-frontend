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

package views.dueandoverduereturns

import base.ViewSpecBase
import models.obligationsandsubmissions._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.html.dueandoverduereturns.DueAndOverdueReturnsView

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}

class DueAndOverdueReturnsViewSpec extends ViewSpecBase {

  lazy val page: DueAndOverdueReturnsView = inject[DueAndOverdueReturnsView]

  val fixedNow:       LocalDate     = LocalDate.now()
  val fromDate:       LocalDate     = LocalDate.of(2023, 1, 1)
  val toDate:         LocalDate     = LocalDate.of(2023, 12, 31)
  val processingDate: ZonedDateTime = ZonedDateTime.now()

  def createObligation(
    obligationType: ObligationType = ObligationType.Pillar2TaxReturn,
    status:         ObligationStatus = ObligationStatus.Open,
    canAmend:       Boolean = true
  ): Obligation =
    Obligation(
      obligationType = obligationType,
      status = status,
      canAmend = canAmend,
      submissions = Seq.empty
    )

  def createAccountingPeriod(
    startDate:    LocalDate = fromDate,
    endDate:      LocalDate = toDate,
    dueDate:      LocalDate,
    underEnquiry: Boolean = false,
    obligations:  Seq[Obligation]
  ): AccountingPeriodDetails =
    AccountingPeriodDetails(
      startDate = startDate,
      endDate = endDate,
      dueDate = dueDate,
      underEnquiry = underEnquiry,
      obligations = obligations
    )

  val emptyData: ObligationsAndSubmissionsSuccess = ObligationsAndSubmissionsSuccess(
    processingDate = processingDate,
    accountingPeriodDetails = Seq.empty
  )

  val allFulfilledData: ObligationsAndSubmissionsSuccess = ObligationsAndSubmissionsSuccess(
    processingDate = processingDate,
    accountingPeriodDetails = Seq(
      createAccountingPeriod(
        dueDate = fixedNow.minusDays(30),
        obligations = Seq(
          createObligation(status = ObligationStatus.Fulfilled),
          createObligation(
            obligationType = ObligationType.GlobeInformationReturn,
            status = ObligationStatus.Fulfilled
          )
        )
      )
    )
  )

  val dueReturnsData: ObligationsAndSubmissionsSuccess = ObligationsAndSubmissionsSuccess(
    processingDate = processingDate,
    accountingPeriodDetails = Seq(
      createAccountingPeriod(
        dueDate = fixedNow.plusDays(30),
        obligations = Seq(
          createObligation()
        )
      )
    )
  )

  val overdueReturnsData: ObligationsAndSubmissionsSuccess = ObligationsAndSubmissionsSuccess(
    processingDate = processingDate,
    accountingPeriodDetails = Seq(
      createAccountingPeriod(
        dueDate = fixedNow.minusDays(30),
        obligations = Seq(
          createObligation()
        )
      )
    )
  )

  val mixedStatusData: ObligationsAndSubmissionsSuccess = ObligationsAndSubmissionsSuccess(
    processingDate = processingDate,
    accountingPeriodDetails = Seq(
      createAccountingPeriod(
        dueDate = fixedNow.plusDays(30),
        obligations = Seq(
          createObligation(),
          createObligation(
            obligationType = ObligationType.GlobeInformationReturn,
            status = ObligationStatus.Fulfilled
          )
        )
      )
    )
  )

  val multiplePeriodsData: ObligationsAndSubmissionsSuccess = ObligationsAndSubmissionsSuccess(
    processingDate = processingDate,
    accountingPeriodDetails = Seq(
      createAccountingPeriod(
        startDate = LocalDate.of(2022, 1, 1),
        endDate = LocalDate.of(2022, 12, 31),
        dueDate = fixedNow.minusDays(60),
        obligations = Seq(
          createObligation()
        )
      ),
      createAccountingPeriod(
        dueDate = fixedNow.plusDays(30),
        obligations = Seq(
          createObligation(),
          createObligation(
            obligationType = ObligationType.GlobeInformationReturn
          )
        )
      )
    )
  )

  def verifyCommonPageElements(view: Document): Unit = {
    view.getElementsByTag("title").get(0).text mustEqual "Due and overdue returns - Report Pillar 2 top-up taxes - GOV.UK"
    view.getElementsByTag("h1").get(0).text mustEqual "Due and overdue returns"

    // Check for submission history section regardless of its position
    val headings = view.getElementsByTag("h2")
    val submissionHistoryHeading =
      headings.stream.filter(h => h.text.contains("Support links") || h.text.contains("Submission history")).findFirst()
    submissionHistoryHeading.isPresent() mustEqual true

    // Look for the paragraph containing the submission history link
    val submissionHistoryParagraph = view.select("p.govuk-body").stream.filter(p => p.text.contains("submission history")).findFirst()
    submissionHistoryParagraph.isPresent() mustEqual true

    val submissionHistoryLink = submissionHistoryParagraph.get().select("a").first()
    submissionHistoryLink.attr("href") must include("/submission-history")
  }

  def verifyTableHeaders(table: org.jsoup.select.Elements): Unit = {
    val headers = table.select("th")
    headers.size() mustEqual 3
    headers.get(0).text mustEqual "Type of return"
    headers.get(1).text mustEqual "Due date"
    headers.get(2).text mustEqual "Status"
  }

  "DueAndOverdueReturnsView" when {
    "there are no returns" must {
      lazy val view: Document = Jsoup.parse(page(emptyData, fromDate, toDate)(request, appConfig, messages).toString())

      "display the common page elements" in {
        verifyCommonPageElements(view)
      }

      "show the 'no returns' message" in {
        val noReturnsMessage = view.getElementsByClass("govuk-body").first()
        noReturnsMessage.text mustEqual "Your group is up to date with their returns for this accounting period."
      }

      "not display any tables" in {
        view.select("table.govuk-table").size mustEqual 0
      }
    }

    "all returns are fulfilled" must {
      lazy val view: Document = Jsoup.parse(page(allFulfilledData, fromDate, toDate)(request, appConfig, messages).toString())

      "display the common page elements" in {
        verifyCommonPageElements(view)
      }

      "show the 'no returns' message (since all are fulfilled)" in {
        val noReturnsMessage = view.getElementsByClass("govuk-body").first()
        noReturnsMessage.text mustEqual "Your group is up to date with their returns for this accounting period."
      }

      "not display any tables or accounting period headings" in {
        view.select("table.govuk-table").size mustEqual 0
        view.select("h2.govuk-heading-s").size mustEqual 0 // No accounting period headings
      }
    }

    "there are due returns" must {
      lazy val view: Document = Jsoup.parse(page(dueReturnsData, fromDate, toDate)(request, appConfig, messages).toString())

      "display the common page elements" in {
        verifyCommonPageElements(view)
      }

      "show the multiple returns information" in {
        val infoMessages = view.select("p.govuk-body")
        infoMessages.get(0).text mustEqual "If you have multiple returns due, they are separated by accounting periods."
        infoMessages.get(1).text mustEqual "You must submit each return before its due date using your commercial software supplier."
      }

      "display the accounting period heading correctly" in {
        val periodHeading = view.getElementsByTag("h2").first()
        periodHeading.text mustEqual "1 January 2023 to 31 December 2023"
      }

      "show a table with properly formatted due returns" in {
        val tables = view.select("table.govuk-table")
        tables.size mustEqual 1

        verifyTableHeaders(tables)

        val cells = tables.select("td")
        cells.get(0).text mustEqual "UK Tax Return"
        cells.get(1).text mustEqual LocalDate.now().plusDays(30).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

        val statusTag = tables.select("td p.govuk-tag")
        statusTag.text mustEqual "Due"
        statusTag.attr("class") must include("govuk-tag--blue")
      }
    }

    "there are overdue returns" must {
      lazy val view: Document = Jsoup.parse(page(overdueReturnsData, fromDate, toDate)(request, appConfig, messages).toString())

      "display the common page elements" in {
        verifyCommonPageElements(view)
      }

      "show a table with properly formatted overdue returns" in {
        val tables = view.select("table.govuk-table")
        tables.size mustEqual 1

        verifyTableHeaders(tables)

        val cells = tables.select("td")
        cells.get(0).text mustEqual "UK Tax Return"
        cells.get(1).text mustEqual LocalDate.now().minusDays(30).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

        val statusTag = tables.select("td p.govuk-tag")
        statusTag.size must be > 0
        statusTag.text mustEqual "Overdue"
        statusTag.attr("class") must include("govuk-tag--red")
      }
    }

    "there is a mix of due and fulfilled returns" must {
      lazy val view: Document = Jsoup.parse(page(mixedStatusData, fromDate, toDate)(request, appConfig, messages).toString())

      "display the common page elements" in {
        verifyCommonPageElements(view)
      }

      "only show due/open returns in the table (not fulfilled ones)" in {
        val tables = view.select("table.govuk-table")
        tables.size mustEqual 1

        val rows = tables.select("tbody tr")
        rows.size mustEqual 1

        val cells = rows.first().select("td")
        cells.get(0).text mustEqual "UK Tax Return"
      }
    }

    "there are multiple accounting periods" must {
      lazy val view: Document = Jsoup.parse(page(multiplePeriodsData, fromDate, toDate)(request, appConfig, messages).toString())

      "display the common page elements" in {
        verifyCommonPageElements(view)
      }

      "show headings for each accounting period with open obligations" in {
        val periodHeadings = view.select("h2.govuk-heading-s")
        periodHeadings.size mustEqual 2
        periodHeadings.get(0).text mustEqual "1 January 2022 to 31 December 2022"
        periodHeadings.get(1).text mustEqual "1 January 2023 to 31 December 2023"
      }

      "display tables for each accounting period with open obligations" in {
        val tables = view.select("table.govuk-table")
        tables.size mustEqual 2

        // First period table - overdue
        val firstTableRows = tables.get(0).select("tbody tr")
        firstTableRows.size mustEqual 1

        val firstTableStatusTag = firstTableRows.first().select("td p.govuk-tag")
        firstTableStatusTag.size must be > 0
        firstTableStatusTag.text mustEqual "Overdue"
        firstTableStatusTag.attr("class") must include("govuk-tag--red")

        // Second period table - due
        val secondTableRows = tables.get(1).select("tbody tr")
        secondTableRows.size mustEqual 2

        val secondTableStatusTags = secondTableRows.select("td p.govuk-tag")
        secondTableStatusTags.size mustEqual 2
        secondTableStatusTags.get(0).text mustEqual "Due"
        secondTableStatusTags.get(0).attr("class") must include("govuk-tag--blue")
        secondTableStatusTags.get(1).text mustEqual "Due"
        secondTableStatusTags.get(1).attr("class") must include("govuk-tag--blue")
      }
    }
  }
}
