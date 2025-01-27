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

package navigation

import base.SpecBase
import controllers.btn.routes._
import controllers.routes
import models._
import pages._

class BTNNavigatorSpec extends SpecBase {

  val navigator = new BTNNavigator

  "BTN Navigator" when {

    "in Normal mode" when {

      "must go from a page that doesn't exist in the route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, UserAnswers("id")) mustBe routes.IndexController.onPageLoad
      }

      "go from EntitiesInsideOutsideUKPage period page to BTNIn2AccountingPeriod page " in {
        navigator.nextPage(EntitiesInsideOutsideUKPage, NormalMode, emptyUserAnswers.setOrException(EntitiesInsideOutsideUKPage, true)) mustBe
          BTNLast4AccountingPeriodsController.onPageLoad(NormalMode)
      }

      "go from EntitiesInsideOutsideUKPage period page to amend group details page when answer is No" in {
        navigator.nextPage(EntitiesInsideOutsideUKPage, NormalMode, emptyUserAnswers.setOrException(EntitiesInsideOutsideUKPage, false)) mustBe
          BTNEntitiesInsideOutsideUKController.onPageLoadAmendGroupDetails()
      }

      "go from  BTNLast4AccountingPeriodsPage period page to UnderConstruction  page " in {
        navigator.nextPage(
          BTNLast4AccountingPeriodsPage,
          NormalMode,
          emptyUserAnswers.setOrException(BTNLast4AccountingPeriodsPage, true)
        ) mustBe
          BTNLast4AccountingPeriodsController.onPageLoadThresholdMet
      }

      "go from BTNLast4AccountingPeriodsPage period page to UnderConstruction page " in {
        navigator.nextPage(
          BTNLast4AccountingPeriodsPage,
          NormalMode,
          emptyUserAnswers.setOrException(BTNLast4AccountingPeriodsPage, false)
        ) mustBe
          BTNNext2AccountingPeriodsController.onPageLoad(NormalMode)
      }

      "go from BTNNext2AccountingPeriodsPage to BTNSubmitUKTRController page when the answer is true" in {
        navigator.nextPage(
          BTNNext2AccountingPeriodsPage,
          NormalMode,
          emptyUserAnswers.setOrException(BTNNext2AccountingPeriodsPage, true)
        ) mustBe
          BTNNext2AccountingPeriodsController.submitUKTRKnockback
      }

      "go from BTNNext2AccountingPeriodsPage to CYA page when the answer is false" in {
        navigator.nextPage(
          BTNNext2AccountingPeriodsPage,
          NormalMode,
          emptyUserAnswers.setOrException(BTNNext2AccountingPeriodsPage, false)
        ) mustBe CheckYourAnswersController.onPageLoad
      }

    }

    "in Check mode" when {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, UserAnswers("id")) mustBe routes.IndexController.onPageLoad
      }
    }
  }
}
