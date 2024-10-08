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
import forms.BtnRevenues750In2AccountingPeriodFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.html.btn.BtnEntitiesInUKOnlyView

class BtnEntitiesInUKOnlyViewSpec extends ViewSpecBase {
  val formProvider = new BtnRevenues750In2AccountingPeriodFormProvider
  val page: BtnEntitiesInUKOnlyView = inject[BtnEntitiesInUKOnlyView]
  val view: Document                = Jsoup.parse(page(formProvider(), NormalMode)(request, appConfig, messages).toString())

  "Btn Entities In UK Only View" should {

    "have a title" in {
      view.getElementsByTag("title").text must include("Does the group still have entities located only in the UK?")
    }

    "have a h1 heading" in {
      view.getElementsByTag("h1").text must include("Does the group still have entities located only in the UK?")
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
