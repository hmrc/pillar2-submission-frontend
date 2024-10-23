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

package viewmodels.checkAnswers

import models.UserAnswers
import pages.SubAccountingPeriodPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.ViewHelpers
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object SubAccountingPeriodSummary {

  private val viewHelpers = new ViewHelpers()

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(SubAccountingPeriodPage).map { answer =>
      val startDate = viewHelpers.formatDateGDS(answer.startDate)
      val endDate   = viewHelpers.formatDateGDS(answer.endDate)

      SummaryListRowViewModel(
        key = "btn.btnAccountingPeriod.checkYourAnswersLabel",
        value = ValueViewModel(
          content = HtmlContent(
            s"""${messages("btn.btnAccountingPeriod.checkYourAnswersLabel.startDate")} $startDate<br>
                 |${messages("btn.btnAccountingPeriod.checkYourAnswersLabel.endDate")} $endDate
                 |""".stripMargin
          )
        )
      )
    }
}
