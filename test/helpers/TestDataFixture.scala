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

package helpers

import models.UserAnswers
import models.subscription.AccountingPeriod
import pages._
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.checkAnswers._
import viewmodels.govuk.all.{FluentSummaryList, SummaryListViewModel}

import java.time.LocalDate

trait TestDataFixture {

  lazy val validBtnCyaUa: UserAnswers = UserAnswers("id")
    .setOrException(SubAccountingPeriodPage, AccountingPeriod(LocalDate.now, LocalDate.now.plusYears(1)))
    .setOrException(EntitiesBothInUKAndOutsidePage, true)
    .setOrException(BtnRevenues750In2AccountingPeriodPage, false)
    .setOrException(BtnRevenues750InNext2AccountingPeriodsPage, false)

  def summaryList(implicit messages: Messages): SummaryList = SummaryListViewModel(
    rows = Seq(
      SubAccountingPeriodSummary.row(validBtnCyaUa),
      BtnEntitiesBothInUKAndOutsideSummary.row(validBtnCyaUa),
      BtnRevenues750In2AccountingPeriodSummary.row(validBtnCyaUa),
      BtnRevenues750InNext2AccountingPeriodsSummary.row(validBtnCyaUa)
    ).flatten
  ).withCssClass("govuk-!-margin-bottom-9")

}
