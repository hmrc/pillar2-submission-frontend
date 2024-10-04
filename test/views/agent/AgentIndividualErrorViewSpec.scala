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
import views.html.agent.AgentIndividualErrorView
import org.jsoup.nodes.Document

class AgentIndividualErrorViewSpec extends ViewSpecBase {

  val page: AgentIndividualErrorView = inject[AgentIndividualErrorView]

  val view: Document = Jsoup.parse(page()(request, appConfig, messages).toString())

  "Agent Individual Error View" should {

    "have a title" in {
      val title = "Sorry, you’re unable to use this service - Report Pillar 2 top-up taxes - GOV.UK"
      view.getElementsByTag("title").first().text mustBe title
    }

    "have a heading" in {
      view.getElementsByTag("h1").text must include("Sorry, you’re unable to use this service")
    }

    "have a paragraph body" in {
      view.getElementsByClass("govuk-body").first().text must include("You’ve signed in with an individual account.")
      view.getElementsByClass("govuk-body").get(1).text  must include("Only users with an agent services account can use this service.")
    }

    "have a paragraph body with links" in {
      val bulletList   = view.getElementsByClass("govuk-list govuk-list--bullet")
      val firstBullet  = bulletList.first().getElementsByClass("govuk-body").first()
      val secondBullet = bulletList.first().getElementsByClass("govuk-body").get(1)

      firstBullet.text() must include(
        "if you are an agent that has been given authorisation to report Pillar 2 top-up taxes on behalf of a group, you must"
      )
      firstBullet.getElementsByTag("a").text() must include("sign in via agent services")
      firstBullet.getElementsByTag("a").attr("href") mustBe "https://www.gov.uk/guidance/sign-in-to-your-agent-services-account"

      secondBullet.text()                       must include("if you need to request authorisation to report Pillar 2 top-up taxes, you must")
      secondBullet.getElementsByTag("a").text() must include("request authorisation on agent services")
      secondBullet
        .getElementsByTag("a")
        .attr("href") mustBe "https://www.gov.uk/guidance/how-to-use-the-online-agent-authorisation-to-get-authorised-as-a-tax-agent"
    }

    "have a link" in {
      val link = view.getElementsByClass("govuk-body").last().getElementsByTag("a")

      link.text         must include("Find out more about who can report for Pillar 2 top-up taxes")
      link.attr("href") must include("https://www.gov.uk/guidance/report-pillar-2-top-up-taxes")
    }

  }
}
