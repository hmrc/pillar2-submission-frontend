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
  val view: Document             = Jsoup.parse(page(btnCyaSummaryList)(request, appConfig, realMessagesApi.preferred(request)).toString())

  "CheckYourAnswersView" must {

    "have a title" in {
      view.getElementsByTag("title").get(0).text mustEqual "Check Your Answers - Report Pillar 2 Top-up Taxes - GOV.UK"
    }

    "have a H1 heading" in {
      view.getElementsByTag("h1").text mustEqual "Check your answers to submit your Below-Threshold Notification"
    }

    "have a paragraph" in {
      view.getElementsByClass("govuk-body").get(0).text mustEqual
        "If you submit a Below-Threshold Notification for a previous accounting period, any return you have submitted this accounting period will be removed."
    }

    "have a paragraph with a H3 heading" in {
      view.getElementsByTag("h3").get(0).text mustEqual "Submit your Below-Threshold Notification"
      view.getElementsByClass("govuk-body").get(1).text mustEqual
        "By submitting these details, you are confirming that the information is correct and complete to the best of your knowledge."
    }

    "have the correct summary list" should {
      "have a summary list keys" in {
        view.getElementsByClass("govuk-summary-list__key").get(0).text mustEqual "Group’s accounting period"
        view.getElementsByClass("govuk-summary-list__key").get(1).text mustEqual
          "Are the entities still located in both the UK and outside the UK?"
      }

      "have a summary list items" in {
        view.getElementsByClass("govuk-summary-list__value").get(0).text mustEqual "Start date: 24 October 2024 End date: 24 October 2025"
        view.getElementsByClass("govuk-summary-list__value").get(1).text mustEqual "Yes"
      }
    }

    "have a 'Confirm and submit' button" in {
      view.getElementsByClass("govuk-button").text mustEqual "Confirm and submit"
    }

  }
}
