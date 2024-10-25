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

package views.btn

import base.ViewSpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.html.btn.CheckYourAnswersView

class CheckYourAnswersViewSpec extends ViewSpecBase {

  val page: CheckYourAnswersView = inject[CheckYourAnswersView]
  val view: Document             = Jsoup.parse(page(summaryList)(request, appConfig, realMessagesApi.preferred(request)).toString())

  "CheckYourAnswersView" must {

    "have a title" in {
      view.getElementsByTag("title").get(0).text mustEqual "Check Your Answers - Report Pillar 2 top-up taxes - GOV.UK"
    }

    "have a H1 heading" in {
      view.getElementsByTag("h1").text mustEqual "Check your answers to submit your Below-Threshold Notification"
    }

    "have a paragraph with a H2 heading" in {
      view.getElementsByTag("h3").get(0).text mustEqual "Now submit your Below Threshold Notification"
      view.getElementsByClass("govuk-body").get(0).text mustEqual
        "By submitting these details, you are confirming that the information is correct and complete to the best of your knowledge."
    }

    "have the correct summary list" should {
      "have a summary list keys" in {
        view.getElementsByClass("govuk-summary-list__key").get(0).text mustEqual "Group’s accounting period"
        view.getElementsByClass("govuk-summary-list__key").get(1).text mustEqual
          "Are the entities still located in both the UK and outside the UK?"

        view.getElementsByClass("govuk-summary-list__key").get(2).text mustEqual
          "Does the group have consolidated annual revenues of €750 million or more in at least 2 of the previous 4 accounting periods?"

        view.getElementsByClass("govuk-summary-list__key").get(3).text mustEqual
          "Is the group expected to make consolidated annual revenues of €750 million or more within the next 2 accounting periods?"

      }

      "have a summary list items" in {
        view.getElementsByClass("govuk-summary-list__value").get(0).text mustEqual "Start date: 24 October 2024 End date: 24 October 2025"
        view.getElementsByClass("govuk-summary-list__value").get(1).text mustEqual "Yes"
        view.getElementsByClass("govuk-summary-list__value").get(2).text mustEqual "No"
        view.getElementsByClass("govuk-summary-list__value").get(3).text mustEqual "No"
      }
    }

    "have a 'Confirm and submit' button" in {
      view.getElementsByClass("govuk-button").text mustEqual "Confirm and submit"
    }

  }
}
