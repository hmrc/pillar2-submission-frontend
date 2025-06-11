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
import models.MneOrDomestic
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.html.btn.BTNAmendDetailsView

class BTNAmendDetailsViewSpec extends ViewSpecBase {

  val page:           BTNAmendDetailsView = inject[BTNAmendDetailsView]
  val viewUkOnly:     Document            = Jsoup.parse(page(MneOrDomestic.Uk)(request, appConfig, messages).toString())
  val viewUkAndOther: Document            = Jsoup.parse(page(MneOrDomestic.UkAndOther)(request, appConfig, messages).toString())

  "BTNAmendDetailsView" should {

    "have a back link" in {
      viewUkOnly.getElementsByClass("govuk-back-link").text     must include("Back")
      viewUkAndOther.getElementsByClass("govuk-back-link").text must include("Back")
    }

    "have a title" in {
      viewUkOnly.getElementsByTag("title").text     must include("Based on your answer, you need to amend your details")
      viewUkAndOther.getElementsByTag("title").text must include("Based on your answer, you need to amend your details")
    }

    "have a h1 heading" in {
      viewUkOnly.getElementsByTag("h1").text     must include("Based on your answer, you need to amend your details")
      viewUkAndOther.getElementsByTag("h1").text must include("Based on your answer, you need to amend your details")
    }

    "have paragraph content" in {
      viewUkOnly.getElementsByClass("govuk-body").first().text() must include(
        "You reported that your group only has entities in the UK."
      )
      viewUkAndOther.getElementsByClass("govuk-body").first().text must include(
        "You reported that your group has entities both in and outside of the UK."
      )

      viewUkOnly.text() must include(
        "If this has changed, you must amend your group details to update the location of your entities before submitting a BTN."
      )

      val linkUkOnly = viewUkOnly.getElementsByClass("govuk-body").last().getElementsByTag("a")
      linkUkOnly.text must include("Amend group details")
      linkUkOnly.attr("href") must include(
        "/report-pillar2-top-up-taxes/manage-account/account-details/summary"
      )

      viewUkAndOther.text() must include(
        "If this has changed, you must amend your group details to update the location of your entities before submitting a BTN."
      )

      val linkUkAndOther = viewUkAndOther.getElementsByClass("govuk-body").last().getElementsByTag("a")
      linkUkAndOther.text must include("Amend group details")
      linkUkAndOther.attr("href") must include(
        "/report-pillar2-top-up-taxes/manage-account/account-details/summary"
      )
    }

  }
}
