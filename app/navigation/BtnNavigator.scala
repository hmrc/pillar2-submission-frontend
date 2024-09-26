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

import models._
import pages._
import play.api.mvc.Call
import controllers.routes
import javax.inject.{Inject, Singleton}

@Singleton
class BtnNavigator @Inject() {

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
  }

  private val normalRoutes: Page => UserAnswers => Call = {
    case EntitiesBothInUKAndOutsidePage             => entitiesBothInUKAndOutside
    case BtnRevenues750In2AccountingPeriodPage      => btnRevenues750In2AccountingPeriod
    case BtnRevenues750InNext2AccountingPeriodsPage => btnRevenues750InNext2AccountingPeriods
    case _                                          => _ => routes.IndexController.onPageLoad
  }

  private def entitiesBothInUKAndOutside(userAnswers: UserAnswers): Call =
    userAnswers
      .get(EntitiesBothInUKAndOutsidePage)
      .map { provided =>
        if (provided) {
          controllers.btn.routes.BtnRevenues750In2AccountingPeriodController.onPageLoad(NormalMode)
        } else {
          controllers.routes.UnderConstructionController.onPageLoad
        }
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private def btnRevenues750In2AccountingPeriod(userAnswers: UserAnswers): Call =
    userAnswers
      .get(BtnRevenues750In2AccountingPeriodPage)
      .map { provided =>
        if (provided) {
          controllers.routes.UnderConstructionController.onPageLoad
        } else {
          controllers.btn.routes.BtnRevenues750InNext2AccountingPeriodsController.onPageLoad(NormalMode)
        }
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())
  private def btnRevenues750InNext2AccountingPeriods(userAnswers: UserAnswers): Call =
    userAnswers
      .get(BtnRevenues750InNext2AccountingPeriodsPage)
      .map { provided =>
        if (provided) {
          controllers.routes.UnderConstructionController.onPageLoad
        } else {
          controllers.routes.UnderConstructionController.onPageLoad
        }
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())

  private val checkRouteMap: Page => UserAnswers => Call = {
    case EntitiesBothInUKAndOutsidePage => _ => controllers.routes.UnderConstructionController.onPageLoad
    case _                              => _ => routes.IndexController.onPageLoad
  }

}
