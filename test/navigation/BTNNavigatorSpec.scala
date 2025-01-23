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
import controllers.routes
import models._
import pages._

class BTNNavigatorSpec extends SpecBase {

  val navigator = new BTNNavigator

  "Btn Navigator" when {

    "in Normal mode" when {

      "must go from a page that doesn't exist in the route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, UserAnswers("id")) mustBe routes.IndexController.onPageLoad
      }

      "go from EntitiesBothInUKAndOutsidePage period page to BtnRevenues750In2AccountingPeriod page " in {
        navigator.nextPage(EntitiesBothInUKAndOutsidePage, NormalMode, emptyUserAnswers.setOrException(EntitiesBothInUKAndOutsidePage, true)) mustBe
          controllers.btn.routes.BTNRevenues750In2AccountingPeriodController.onPageLoad(NormalMode)
      }

      "go from EntitiesBothInUKAndOutsidePage period page to amend group details page when answer is No" in {
        navigator.nextPage(EntitiesBothInUKAndOutsidePage, NormalMode, emptyUserAnswers.setOrException(EntitiesBothInUKAndOutsidePage, false)) mustBe
          controllers.btn.routes.BTNEntitiesBothInUKAndOutsideController.onPageLoadAmendGroupDetails()
      }

      "go from  BTNRevenues750In2AccountingPeriodPage period page to UnderConstruction  page " in {
        navigator.nextPage(
          BTNRevenues750In2AccountingPeriodPage,
          NormalMode,
          emptyUserAnswers.setOrException(BTNRevenues750In2AccountingPeriodPage, true)
        ) mustBe
          controllers.btn.routes.BTNRevenues750In2AccountingPeriodController.onPageLoadThresholdMet
      }

      "go from  BTNRevenues750In2AccountingPeriodPage period page to UnderConstruction page " in {
        navigator.nextPage(
          BTNRevenues750In2AccountingPeriodPage,
          NormalMode,
          emptyUserAnswers.setOrException(BTNRevenues750In2AccountingPeriodPage, false)
        ) mustBe
          controllers.btn.routes.BTNRevenues750InNext2AccountingPeriodsController.onPageLoad(NormalMode)
      }

      "go from BTNRevenues750InNext2AccountingPeriodsPage to BTNSubmitUKTRController page when the answer is true" in {
        navigator.nextPage(
          BTNRevenues750InNext2AccountingPeriodsPage,
          NormalMode,
          emptyUserAnswers.setOrException(BTNRevenues750InNext2AccountingPeriodsPage, true)
        ) mustBe
          controllers.btn.routes.BTNSubmitUKTRController.onPageLoad
      }

      "go from BTNRevenues750InNext2AccountingPeriodsPage to CYA page when the answer is false" in {
        navigator.nextPage(
          BTNRevenues750InNext2AccountingPeriodsPage,
          NormalMode,
          emptyUserAnswers.setOrException(BTNRevenues750InNext2AccountingPeriodsPage, false)
        ) mustBe controllers.btn.routes.CheckYourAnswersController.onPageLoad
      }

      "redirect to JourneyRecoveryController when user answers are missing" in {
        navigator.nextPage(
          EntitiesBothInUKAndOutsidePage,
          NormalMode,
          emptyUserAnswers
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
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
