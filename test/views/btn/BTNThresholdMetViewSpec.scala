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
import views.html.btn.BTNThresholdMetView

class BTNThresholdMetViewSpec extends ViewSpecBase {

  val page: BTNThresholdMetView = inject[BTNThresholdMetView]
  val view: Document            = Jsoup.parse(page()(request, appConfig, messages).toString())

  "BTNThresholdMetView" should {

    "have a back link" in {
      view.getElementsByClass("govuk-back-link").text must include("Back")
    }

    "have a title" in {
      view.getElementsByTag("title").text must include("Based on your answer, your group must submit a UK Tax Return")
    }

    "have a h1 heading" in {
      view.getElementsByTag("h1").text must include("Based on your answer, your group must submit a UK Tax Return")
    }

    "have paragraph content" in {
      view.getElementsByClass("govuk-body").first().text() must include(
        "Based on your answer, you cannot submit a Below-Threshold Notification as your group has made consolidated annual revenues of €750 million or more in at least 2 of the previous 4 accounting periods."
      )
      view.getElementsByClass("govuk-body").get(1).text() must include(
        "Your group must still submit a UK Tax Return."
      )
    }

    "have a link" in {
      val link = view.getElementsByClass("govuk-body").last().getElementsByTag("a")

      link.text         must include("Find out more about submitting a UKTR")
      link.attr("href") must include("/report-pillar2-submission-top-up-taxes/uk-tax-return")
    }

  }
}
