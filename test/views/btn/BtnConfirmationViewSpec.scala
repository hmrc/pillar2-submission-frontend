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
import views.html.btn.BtnConfirmationView

class BtnConfirmationViewSpec extends ViewSpecBase {

  private val currentDate = "10 November 2024"
  private val startDate   = "11 November 2024"

  val page: BtnConfirmationView = inject[BtnConfirmationView]
  val view: Document            = Jsoup.parse(page(currentDate, startDate)(request, appConfig, messages).toString())

  "Btn Confirmation View" should {

    "have a title" in {
      view.getElementsByTag("title").text must include("Below-Threshold Notification successful")
    }

    "have a h1 heading" in {
      view.getElementsByTag("h1").text must include("Below-Threshold Notification successful")
    }

    "have a h2 heading" in {
      view.getElementsByTag("h2").text() must include("What happens next")
    }

    "have paragraph content" in {
      view.getElementsByClass("govuk-body").text must include(
        s"You have submitted a Below-Threshold Notification on $currentDate. This will be effective from $startDate."
      )

      view.getElementsByClass("govuk-body").text must include(
        "HMRC has removed your group’s obligation to submit a UK Tax Return for this current accounting period and any future ones."
      )

      view.getElementsByClass("govuk-body").text must include(
        "If your group becomes liable for a UK Tax Return in the future, you must let HMRC know by submitting a UK Tax Return to remove the Below-Threshold Notification from your account."
      )
    }

    "have links" in {
      val linkOne = view.getElementsByClass("govuk-body").get(2).getElementsByTag("a")
      val linkTwo = view.getElementsByClass("govuk-body").get(3).getElementsByTag("a")

      linkOne.text         must include("find out more about submitting a UKTR")
      linkOne.attr("href") must include("/uk-tax-return")

      linkTwo.text()       must include("View account homepage")
      linkTwo.attr("href") must include("/report-pillar2-top-up-taxes")
    }
  }
}
