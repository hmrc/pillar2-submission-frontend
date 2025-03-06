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

import models.obligationsandsubmissions._
import utils.Constants.SUBMISSION_ACCOUNTING_PERIODS

import java.time.{LocalDate, ZonedDateTime}

trait DueAndOverdueReturnsDataFixture {

  // Use the current date as the base for our tests
  val currentDate: LocalDate = LocalDate.now()
  val fromDate:    LocalDate = currentDate.minusYears(SUBMISSION_ACCOUNTING_PERIODS)
  val toDate:      LocalDate = currentDate

  // Calculate dates that will always be in the past or future
  val pastDueDate:   LocalDate = currentDate.minusDays(30) // Always overdue
  val futureDueDate: LocalDate = currentDate.plusDays(30) // Always due

  def createObligation(
    obligationType: ObligationType = ObligationType.Pillar2TaxReturn,
    status:         ObligationStatus = ObligationStatus.Open,
    canAmend:       Boolean = true
  ): Obligation =
    Obligation(
      obligationType = obligationType,
      status = status,
      canAmend = canAmend,
      submissions = Seq.empty
    )

  def createAccountingPeriod(
    startDate:    LocalDate = fromDate,
    endDate:      LocalDate = toDate,
    dueDate:      LocalDate,
    underEnquiry: Boolean = false,
    obligations:  Seq[Obligation]
  ): AccountingPeriodDetails =
    AccountingPeriodDetails(
      startDate = startDate,
      endDate = endDate,
      dueDate = dueDate,
      underEnquiry = underEnquiry,
      obligations = obligations
    )

  val emptyResponse: ObligationsAndSubmissionsSuccess = ObligationsAndSubmissionsSuccess(
    processingDate = ZonedDateTime.now(),
    accountingPeriodDetails = Seq.empty
  )

  val allFulfilledResponse: ObligationsAndSubmissionsSuccess = ObligationsAndSubmissionsSuccess(
    processingDate = ZonedDateTime.now(),
    accountingPeriodDetails = Seq(
      createAccountingPeriod(
        dueDate = pastDueDate,
        obligations = Seq(
          createObligation(status = ObligationStatus.Fulfilled),
          createObligation(
            obligationType = ObligationType.GlobeInformationReturn,
            status = ObligationStatus.Fulfilled
          )
        )
      )
    )
  )

  val dueReturnsResponse: ObligationsAndSubmissionsSuccess = ObligationsAndSubmissionsSuccess(
    processingDate = ZonedDateTime.now(),
    accountingPeriodDetails = Seq(
      createAccountingPeriod(
        dueDate = futureDueDate,
        obligations = Seq(
          createObligation()
        )
      )
    )
  )

  val overdueReturnsResponse: ObligationsAndSubmissionsSuccess = ObligationsAndSubmissionsSuccess(
    processingDate = ZonedDateTime.now(),
    accountingPeriodDetails = Seq(
      createAccountingPeriod(
        dueDate = pastDueDate,
        obligations = Seq(
          createObligation()
        )
      )
    )
  )

  val mixedStatusResponse: ObligationsAndSubmissionsSuccess = ObligationsAndSubmissionsSuccess(
    processingDate = ZonedDateTime.now(),
    accountingPeriodDetails = Seq(
      createAccountingPeriod(
        dueDate = futureDueDate,
        obligations = Seq(
          createObligation(),
          createObligation(
            obligationType = ObligationType.GlobeInformationReturn,
            status = ObligationStatus.Fulfilled
          )
        )
      )
    )
  )

  val multiplePeriodsResponse: ObligationsAndSubmissionsSuccess = ObligationsAndSubmissionsSuccess(
    processingDate = ZonedDateTime.now(),
    accountingPeriodDetails = Seq(
      createAccountingPeriod(
        startDate = currentDate.minusYears(1).withMonth(1).withDayOfMonth(1),
        endDate = currentDate.minusYears(1).withMonth(12).withDayOfMonth(31),
        dueDate = pastDueDate,
        obligations = Seq(
          createObligation()
        )
      ),
      createAccountingPeriod(
        dueDate = futureDueDate,
        obligations = Seq(
          createObligation(),
          createObligation(
            obligationType = ObligationType.GlobeInformationReturn
          )
        )
      )
    )
  )
}
