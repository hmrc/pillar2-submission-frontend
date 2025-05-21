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

package controllers

import models.obligationsandsubmissions.ObligationStatus.Open
import models.obligationsandsubmissions.ObligationType.UKTR
import models.obligationsandsubmissions.{AccountingPeriodDetails, Obligation}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import java.time.LocalDate

class FilteredAccountingPeriodDetailsSpec extends AnyFreeSpec with Matchers {

  private val now        = LocalDate.now
  private def minusYears(amountOfYears: Int) = LocalDate.now.minusYears(amountOfYears)
  private def plusYears(amountOfYears: Int) = LocalDate.now.plusYears(amountOfYears)

  private val obligationData: Seq[Obligation] = Seq(Obligation(UKTR, Open, canAmend = false, Seq.empty))

  private val unfilteredList: Seq[AccountingPeriodDetails] = Seq(
    AccountingPeriodDetails(minusYears(3), minusYears(2), minusYears(1), underEnquiry = false, obligationData),
    AccountingPeriodDetails(minusYears(2), minusYears(1), now, underEnquiry = false, obligationData),
    AccountingPeriodDetails(minusYears(1), now, plusYears(1), underEnquiry = false, obligationData),
    AccountingPeriodDetails(now, plusYears(1), plusYears(2), underEnquiry = false, obligationData),
    AccountingPeriodDetails(plusYears(1), plusYears(2), plusYears(3), underEnquiry = false, obligationData)
  )

  private val filteredList: Seq[AccountingPeriodDetails] = Seq(
    AccountingPeriodDetails(minusYears(2), minusYears(1), now, underEnquiry = false, obligationData),
    AccountingPeriodDetails(minusYears(1), now, plusYears(1), underEnquiry = false, obligationData),
    AccountingPeriodDetails(now, plusYears(1), plusYears(2), underEnquiry = false, obligationData)
  )

  "filteredAccountingPeriodDetails" - {

    "must return a filtered sequence of AccountingPeriodDetails" in {
      filteredAccountingPeriodDetails(unfilteredList) mustEqual filteredList
    }
  }
}
