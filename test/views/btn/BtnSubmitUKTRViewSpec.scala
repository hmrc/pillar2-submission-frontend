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

import org.jsoup.Jsoup
import utils.ViewSpecBase
import views.html.btn.BtnSubmitUKTRView

class BtnSubmitUKTRViewSpec extends ViewSpecBase {

  val page = inject[BtnSubmitUKTRView]

  val view = Jsoup.parse(page()(request, appConfig, messages).toString())

  "BtnSubmitUKTR View" should {

    "have a title" in {
      view.getElementsByTag("title").text must include(messages("btn.btnSubmitUKTR.title"))
    }

    "have a h1 heading" in {
      view.getElementsByTag("h1").text must include(messages("btn.btnSubmitUKTR.heading"))
    }

    "have paragraphs" in {
      view.getElementsByClass("govuk-body").text must include(messages("btn.btnSubmitUKTR.p1"))
      view.getElementsByClass("govuk-body").text must include(messages("btn.btnSubmitUKTR.p2"))
    }

    "have a link" in {
      val link = view.getElementsByClass("govuk-body").last().getElementsByTag("a")
      link.attr("href") must include(controllers.uktr.routes.UkTaxReturnStartController.onPageLoad.url)
      link.text         must include(messages("btn.btnSubmitUKTR.link"))
    }
  }
}
