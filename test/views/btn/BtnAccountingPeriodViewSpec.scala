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
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import views.html.btn.BtnAccountingPeriodView

class BtnAccountingPeriodViewSpec extends ViewSpecBase {
  val list = SummaryListViewModel(
    rows = Seq(
      SummaryListRowViewModel("btn.btnAccountingPeriod.startAccountDate", value = ValueViewModel(HtmlContent(HtmlFormat.escape("7 January 2024")))),
      SummaryListRowViewModel(
        "btn.btnAccountingPeriod.endAccountDate",
        value = ValueViewModel(HtmlContent(HtmlFormat.escape("7 January 2025").toString))
      )
    )
  )

  val page: BtnAccountingPeriodView = inject[BtnAccountingPeriodView]
  val view: Document                = Jsoup.parse(page(list, NormalMode, "test-url")(request, appConfig, messages).toString())

  "Btn Before Start View" should {

    "have a title" in {
      view.getElementsByTag("title").text must include("Submit a Below-Threshold Notification for your group’s current accounting period")
    }

    "have a h1 heading" in {
      view.getElementsByTag("h1").text must include("Submit a Below-Threshold Notification for your group’s current accounting period")
    }

    "have following contents" in {
      view.getElementsByClass("govuk-body").text must include(
        "The Below-Threshold Notification you submit will remove your group’s obligation to submit a UKTR for this current accounting period and all future ones."
      )
      view.getElementsByClass("govuk-summary-list__key").text must include(
        "Start date of accounting period"
      )
      view.getElementsByClass("govuk-summary-list__key").text must include(
        "End date of accounting period"
      )
    }

    "have a button" in {
      view.getElementsByClass("govuk-button").text must include("Continue")
    }

  }
}
