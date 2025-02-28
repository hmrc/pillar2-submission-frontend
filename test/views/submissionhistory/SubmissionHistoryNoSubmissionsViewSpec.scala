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

package views.submissionhistory

import base.ViewSpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.html.submissionhistory.SubmissionHistoryNoSubmissionsView

class SubmissionHistoryNoSubmissionsViewSpec extends ViewSpecBase {

  val page: SubmissionHistoryNoSubmissionsView = inject[SubmissionHistoryNoSubmissionsView]

  val view: Document = Jsoup.parse(page()(request, appConfig, messages).toString())

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
        "No submissions made."
      )
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
      link.attr("href") mustEqual "#" //TODO: Change URL when due and overdue returns page is built
    }
  }
}
