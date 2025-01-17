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
import forms.BTNRevenues750InNext2AccountingPeriodsFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.html.btn.BTNRevenues750InNext2AccountingPeriodsView

class BTNRevenues750InNext2AccountingPeriodsViewSpec extends ViewSpecBase {
  val formProvider = new BTNRevenues750InNext2AccountingPeriodsFormProvider
  val page: BTNRevenues750InNext2AccountingPeriodsView = inject[BTNRevenues750InNext2AccountingPeriodsView]
  val view: Document                                   = Jsoup.parse(page(formProvider(), NormalMode)(request, appConfig, messages).toString())

  "Btn Revenues750 In Next 2 Accounting Periods View" should {

    "have a title" in {
      view.getElementsByTag("title").text must include(
        "Is the group expected to make consolidated annual revenues of €750 million or more within the next 2 accounting periods?"
      )
    }
    "have radio items" in {
      view.getElementsByClass("govuk-label govuk-radios__label").get(0).text must include("Yes")
      view.getElementsByClass("govuk-label govuk-radios__label").get(1).text must include("No")
    }

    "have a button" in {
      view.getElementsByClass("govuk-button").text must include("Continue")
    }

  }
}
