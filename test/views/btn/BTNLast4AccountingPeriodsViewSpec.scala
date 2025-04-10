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
import forms.BTNLast4AccountingPeriodFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.html.btn.BTNLast4AccountingPeriodsView

class BTNLast4AccountingPeriodsViewSpec extends ViewSpecBase {
  val formProvider = new BTNLast4AccountingPeriodFormProvider
  val page: BTNLast4AccountingPeriodsView = inject[BTNLast4AccountingPeriodsView]
  val view: Document                      = Jsoup.parse(page(formProvider(), NormalMode)(request, appConfig, messages).toString())

  "BTN Revenues 750 In 2 Accounting Period View" should {

    "have a title" in {
      view.getElementsByTag("title").text must include(
        "Did the group have consolidated annual revenues of €750 million or more in at least 2 of the previous 4 accounting periods?"
      )
    }

    "have a h1 heading" in {
      view.getElementsByTag("h1").text must include(
        "Did the group have consolidated annual revenues of €750 million or more in at least 2 of the previous 4 accounting periods?"
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
