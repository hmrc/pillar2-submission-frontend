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
import views.html.BtnBeforeStartView

class BtnBeforeStartViewSpec extends ViewSpecBase {

  val page: BtnBeforeStartView = inject[BtnBeforeStartView]
  val view: Document           = Jsoup.parse(page(NormalMode)(request, appConfig, messages).toString())

  "Btn Before Start View" should {

    "have a title" in {
      view.getElementsByTag("title").text must include("Below Threshold Notification")
    }

    "have a h1 heading" in {
      view.getElementsByTag("h1").text must include("Below Threshold Notification")
    }

    "have two h2 headings" in {
      view.getElementsByTag("h2").text must include("Who can submit a Below-Threshold Notification")
      view.getElementsByTag("h2").text must include("Before you start")
    }

    "have following contents" in {
      view.getElementsByClass("govuk-body").text must include(
        "A Below-Threshold Notification removes your group’s obligation to submit a UKTR for the current accounting period and all future ones."
      )

      view.getElementsByClass("govuk-body").text must include(
        "You can submit a Below-Threshold Notification if the group:"
      )

      view.getElementsByClass("govuk-body").text must include(
        "To submit a Below-Threshold Notification you will need to tell us:"
      )
      view.getElementsByTag("li").text must include(
        "does not have consolidated annual revenues of €750 million or more in at least 2 of the previous 4 accounting periods"
      )
      view.getElementsByTag("li").text must include(
        "is not expected to make consolidated annual revenues of €750 million or more within the next 2 accounting periods"
      )
      view.getElementsByTag("li").text must include("the start and end date of the group’s accounting period")
      view.getElementsByTag("li").text must include(
        "whether the group’s consolidated annual revenues have been €750 million or more in at least 2 of the previous 4 accounting periods"
      )
      view.getElementsByTag("li").text must include(
        "whether the group is expected to make consolidated annual revenues of €750 million or more within the next 2 accounting periods "
      )
    }

    "have a button" in {
      view.getElementsByClass("govuk-button").text must include("Continue")
    }

  }
}
