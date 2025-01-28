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

package views.uktr

import base.ViewSpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.html.uktr.UKTaxReturnStartView

class UKTaxReturnStartViewSpec extends ViewSpecBase {

  val page: UKTaxReturnStartView = inject[UKTaxReturnStartView]

  "UK Tax Return Start View with inactiveStatus false" should {

    val inactiveStatus: Boolean  = false
    val view:           Document = Jsoup.parse(page(inactiveStatus)(request, appConfig, messages).toString())

    "have a title" in {
      view.getElementsByTag("title").text must include("UK Tax Return (UKTR)")
    }

    "not have a notification banner" in {
      view.getElementsByClass("govuk-notification-banner__title").text() mustNot include("Important")
      view.getElementsByClass("govuk-notification-banner__heading").text() mustNot include(
        "Your account has a Below Threshold Notification (BTN) and is inactive. " +
          "Submit a UK tax return (UKTR) to re-activate your account."
      )
    }

    "have a h1 heading" in {
      view.getElementsByTag("h1").text must include("UK Tax Return (UKTR)")
    }

    "have paragraphs" in {
      view.getElementsByClass("govuk-body").get(0).text must include(
        "A UKTR fulfills your obligation to submit Pillar 2 top-up taxes for your current accounting period. " +
          "You can report both Domestic Top-up Tax and Multinational Top-up Tax liabilities."
      )

      view.getElementsByClass("govuk-body").get(1).text must include(
        "If your group only has entities located in the UK, your return will include Domestic Top-up Tax."
      )

      view.getElementsByClass("govuk-body").get(2).text must include(
        "If your group has entities located both inside and outside the UK, " +
          "your return will include both Domestic Top-up Tax and Multinational Top-up Tax."
      )

      view.getElementsByClass("govuk-body").get(3).text must include(
        "You must submit a UKTR via a third party software provider. Before you submit your UKTR,"
      )

      view.getElementsByClass("govuk-body").get(4).text must include(
        "HMRC cannot recommend or endorse any one service over another " +
          "and will not be responsible for any loss, damage, cost or expense in connection with using the software."
      )

    }

    "have a subheading" in {
      view.getElementsByTag("h2").text must include(
        "How to submit a UKTR"
      )
    }

    "have links" in {
      val link = view.getElementsByClass("govuk-body").get(3).getElementsByTag("a")
      link.attr("href") must include("/report-pillar2-top-up-taxes/manage-account/account-details/summary")
      link.text         must include("ensure your group’s details are updated")
      val link2 = view.getElementsByClass("govuk-body").get(5).getElementsByTag("a")
      link2.attr("href") must include("/guidance/report-pillar-2-top-up-taxes")
      link2.text         must include("Choose a supplier to submit your UKTR from this list")
    }

  }

  "UK Tax Return Start View with inactiveStatus true" should {

    val inactiveStatus: Boolean  = true
    val view:           Document = Jsoup.parse(page(inactiveStatus)(request, appConfig, messages).toString())

    "have a title" in {
      view.getElementsByTag("title").text must include("UK Tax Return (UKTR)")
    }

    "have a notification banner" in {
      view.getElementsByClass("govuk-notification-banner__title").text() must include("Important")
      view.getElementsByClass("govuk-notification-banner__heading").text() must include(
        "Your account has a Below Threshold Notification (BTN) and is inactive. " +
          "Submit a UK tax return (UKTR) to re-activate your account."
      )
    }

    "have a h1 heading" in {
      view.getElementsByTag("h1").text must include("UK Tax Return (UKTR)")
    }

    "have paragraphs" in {
      view.getElementsByClass("govuk-body").get(0).text must include(
        "A UKTR fulfills your obligation to submit Pillar 2 top-up taxes for your current accounting period. " +
          "You can report both Domestic Top-up Tax and Multinational Top-up Tax liabilities."
      )

      view.getElementsByClass("govuk-body").get(1).text must include(
        "If your group only has entities located in the UK, your return will include Domestic Top-up Tax."
      )

      view.getElementsByClass("govuk-body").get(2).text must include(
        "If your group has entities located both inside and outside the UK, " +
          "your return will include both Domestic Top-up Tax and Multinational Top-up Tax."
      )

      view.getElementsByClass("govuk-body").get(3).text must include(
        "You must submit a UKTR via a third party software provider. Before you submit your UKTR,"
      )

      view.getElementsByClass("govuk-body").get(4).text must include(
        "HMRC cannot recommend or endorse any one service over another " +
          "and will not be responsible for any loss, damage, cost or expense in connection with using the software."
      )

    }

    "have a subheading" in {
      view.getElementsByTag("h2").text must include(
        "How to submit a UKTR"
      )
    }

    "have links" in {
      val link = view.getElementsByClass("govuk-body").get(3).getElementsByTag("a")
      link.attr("href") must include("/report-pillar2-top-up-taxes/manage-account/account-details/summary")
      link.text         must include("ensure your group’s details are updated")
      val link2 = view.getElementsByClass("govuk-body").get(5).getElementsByTag("a")
      link2.attr("href") must include("/guidance/report-pillar-2-top-up-taxes")
      link2.text         must include("Choose a supplier to submit your UKTR from this list")
    }

  }

}
