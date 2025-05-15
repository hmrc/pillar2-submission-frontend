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
import views.html.btn.BTNConfirmationView

class BTNConfirmationViewSpec extends ViewSpecBase {

  private val currentDate = "10 November 2024"
  private val startDate   = "11 November 2024"

  val page: BTNConfirmationView = inject[BTNConfirmationView]
  val view: Document            = Jsoup.parse(page(currentDate, startDate)(request, appConfig, messages).toString())

  "BTNConfirmationView" should {

    "have a title" in {
      view.getElementsByTag("title").text must include("Below-Threshold Notification successful")
    }

    "have no back link" in {
      view.getElementsByClass("govuk-back-link").size mustBe 0
    }

    "have a h1 heading" in {
      view.getElementsByTag("h1").text must include("Below-Threshold Notification successful")
    }

    "have a h2 heading" in {
      view.getElementsByTag("h2").text() must include("What happens next")
    }

    "in an organisation flow have paragraph content" in {
      view.getElementsByClass("govuk-body").text must include(
        s"You have submitted a Below-Threshold Notification on $currentDate."
      )

      view.getElementsByClass("govuk-body").text must include(
        s"This is effective from the start of the accounting period you selected, $startDate."
      )

      view.getElementsByClass("govuk-body").text must include(
        "This Below-Threshold Notification satisfies your group’s obligation to submit a UKTR for the current and future accounting periods. HMRC will not expect to receive an information return while your group remains below-threshold."
      )

      view.getElementsByClass("govuk-body").text must include(
        "You must submit a UK Tax Return if your group meets the threshold conditions in the future."
      )
    }

    "have links" in {
      val linkOne = view.getElementsByClass("govuk-body").get(4).getElementsByTag("a")

      linkOne.text()       must include("Back to group’s homepage")
      linkOne.attr("href") must include("/report-pillar2-top-up-taxes")
    }
  }
}
