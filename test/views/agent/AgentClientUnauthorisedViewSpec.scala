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

package views.agent

import org.jsoup.Jsoup
import utils.ViewSpecBase
import views.html.agent.AgentClientUnauthorisedView

class AgentClientUnauthorisedViewSpec extends ViewSpecBase {

  val page = inject[AgentClientUnauthorisedView]

  val view = Jsoup.parse(page()(request, appConfig, messages).toString())

  "Agent Error View" should {

    "have a title" in {
      val title = "You have not been authorised to report this client’s Pillar 2 top-up taxes - Report Pillar 2 top-up taxes - GOV.UK"
      view.getElementsByTag("title").first().text mustBe title
    }

    "have a heading" in {
      view.getElementsByTag("h1").text must include("You have not been authorised to report this client’s Pillar 2 top-up taxes")
    }

    "have a paragraph body" in {
      view.getElementsByClass("govuk-body").first().text must include("You need to")
    }

    "have a link" in {
      val link = view.getElementsByClass("govuk-body").last().getElementsByTag("a")

      link.text         must include("request authorisation to report and manage this client’s Pillar 2 top-up taxes")
      link.attr("href") must include("/report-pillar2-submission-top-up-taxes/asa/home")
    }

  }
}
