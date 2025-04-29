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
import models.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.html.btn.BTNBeforeStartView

class BTNBeforeStartViewSpec extends ViewSpecBase {

  val page:                           BTNBeforeStartView = inject[BTNBeforeStartView]
  def view(isAgent: Boolean = false): Document           = Jsoup.parse(page(isAgent, NormalMode)(request, appConfig, messages).toString())

  "BTNBeforeStartView" should {

    "have a title" in {
      view().getElementsByTag("title").text must include("Below-Threshold Notification (BTN)")
    }

    "have a h1 heading" in {
      view().getElementsByTag("h1").text must include("Below-Threshold Notification (BTN)")
    }

    "have two h2 headings" in {
      view().getElementsByTag("h2").text must include("Who can submit a Below-Threshold Notification")
      view().getElementsByTag("h2").text must include("Before you start")
    }

    "have group specific content" in {
      view().getElementsByClass("govuk-body").text must include(
        "A Below-Threshold Notification (BTN) removes your group’s obligation to submit a UKTR for the current and future accounting periods. HMRC will not expect to receive an information return while your group is below-threshold."
      )

      view().getElementsByClass("govuk-body").text must include(
        "You can submit a Below-Threshold Notification if the group:"
      )

      view().getElementsByClass("govuk-inset-text").text must include(
        "If you need to submit a UK tax return for this accounting period you do not qualify for a Below-Threshold Notification."
      )
    }

    "have agent specific content" in {
      view(true).getElementsByClass("govuk-body").text must include(
        "A Below-Threshold Notification (BTN) removes your client’s obligation to submit a UKTR for the current and future accounting periods. HMRC will not expect to receive an information return while your client is below-threshold."
      )

      view(true).getElementsByClass("govuk-body").text must include(
        "The group can submit a Below-Threshold Notification if it:"
      )

      view(true).getElementsByClass("govuk-inset-text").text must include(
        "If your client needs to submit a UK tax return for this accounting period they do not qualify for a Below-Threshold Notification."
      )
    }

    "have the following common content" in {
      view().getElementsByClass("govuk-body").text must include(
        "To submit a Below-Threshold Notification you’ll need to tell us:"
      )
      view().getElementsByTag("li").text must include(
        "does not have consolidated annual revenues of €750 million or more in at least 2 of the previous 4 accounting periods"
      )
      view().getElementsByTag("li").text must include(
        "is not expected to make consolidated annual revenues of €750 million or more within the next 2 accounting periods"
      )
      view().getElementsByTag("li").text must include("the start and end date of the group’s accounting period")
      view().getElementsByTag("li").text must include("whether the group has entities located in the UK")
      view().getElementsByClass("govuk-body").text must include(
        "If you submit a Below-Threshold Notification, this will replace any returns you’ve submitted for that period. It will also replace any returns you have already submitted for your most recent account periods."
      )
    }

    "have a button" in {
      view().getElementsByClass("govuk-button").text must include("Continue")
    }

  }
}
