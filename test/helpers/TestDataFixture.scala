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

import models.btn.BTNStatus
import models.obligationsandsubmissions.ObligationStatus.Fulfilled
import models.obligationsandsubmissions.SubmissionType.UKTR_CREATE
import models.obligationsandsubmissions._
import models.subscription._
import models.{MneOrDomestic, NonUKAddress, UserAnswers}
import pages._
import play.api.i18n.Messages
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.checkAnswers._
import viewmodels.govuk.all.{FluentSummaryList, SummaryListViewModel}

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, ZoneOffset, ZonedDateTime}

trait TestDataFixture extends SubscriptionLocalDataFixture {

  lazy val testZonedDateTime: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS)

  lazy val obligationsAndSubmissionsSuccessResponseJson: JsValue = Json.toJson(obligationsAndSubmissionsSuccessResponse().success)

  lazy val submittedBTNRecord: UserAnswers = validBTNCyaUa.set(BTNStatus, BTNStatus.submitted).get

  lazy val validBTNCyaUa: UserAnswers = UserAnswers("id")
    .setOrException(SubAccountingPeriodPage, accountingPeriod)
    .setOrException(EntitiesInsideOutsideUKPage, true)

  def btnCyaSummaryList(implicit messages: Messages): SummaryList = SummaryListViewModel(
    rows = Seq(
      SubAccountingPeriodSummary.row(accountingPeriod),
      BTNEntitiesInsideOutsideUKSummary.row(validBTNCyaUa)
    ).flatten
  ).withCssClass("govuk-!-margin-bottom-9")

  def obligationsAndSubmissionsSuccessResponse(
    underEnquiry:   Boolean = false,
    obligationType: ObligationType = ObligationType.UKTR,
    status:         ObligationStatus = Fulfilled,
    canAmend:       Boolean = true,
    submissionType: SubmissionType = UKTR_CREATE,
    receivedDate:   ZonedDateTime = testZonedDateTime,
    country:        Option[String] = None
  ): ObligationsAndSubmissionsSuccessResponse =
    ObligationsAndSubmissionsSuccessResponse(
      ObligationsAndSubmissionsSuccess(
        processingDate = testZonedDateTime,
        accountingPeriodDetails = Seq(
          AccountingPeriodDetails(
            startDate = localDateFrom,
            endDate = localDateTo,
            dueDate = localDateTo.plusMonths(10),
            underEnquiry = underEnquiry,
            obligations = Seq(
              Obligation(
                obligationType = obligationType,
                status = status,
                canAmend = canAmend,
                submissions = Some(
                  Seq(
                    Submission(submissionType = submissionType, receivedDate = receivedDate, country = country)
                  )
                )
              )
            )
          )
        )
      )
    )
}

trait SubscriptionLocalDataFixture {
  lazy val localDateFrom: LocalDate = LocalDate.of(2024, 10, 24)
  lazy val localDateTo:   LocalDate = localDateFrom.plusYears(1)

  private val upeCorrespondenceAddress = UpeCorrespAddressDetails("middle", None, Some("lane"), None, None, "obv")
  private val contactDetails           = ContactDetailsType("shadow", Some("dota2"), "shadow@fiend.com")
  val accountingPeriod: AccountingPeriod = AccountingPeriod(localDateFrom, localDateTo)

  val subscriptionData: SubscriptionData = SubscriptionData(
    formBundleNumber = "form bundle",
    upeDetails = UpeDetails(None, None, None, "orgName", localDateFrom, domesticOnly = false, filingMember = false),
    upeCorrespAddressDetails = upeCorrespondenceAddress,
    primaryContactDetails = contactDetails,
    secondaryContactDetails = None,
    filingMemberDetails = None,
    accountingPeriod = accountingPeriod,
    accountStatus = Some(AccountStatus(false))
  )

  val someSubscriptionLocalData: SubscriptionLocalData = SubscriptionLocalData(
    plrReference = "Abc123",
    subMneOrDomestic = MneOrDomestic.Uk,
    subAccountingPeriod = accountingPeriod,
    subPrimaryContactName = "John",
    subPrimaryEmail = "john@email.com",
    subPrimaryPhonePreference = true,
    subPrimaryCapturePhone = Some("123"),
    subAddSecondaryContact = true,
    subSecondaryContactName = Some("Doe"),
    subSecondaryEmail = Some("doe@email.com"),
    subSecondaryCapturePhone = Some("123"),
    subSecondaryPhonePreference = Some(true),
    subRegisteredAddress = NonUKAddress("line1", None, "line", None, None, "GB"),
    accountStatus = Some(AccountStatus(false))
  )

  val someSubscriptionLocalDataUkOther: SubscriptionLocalData = SubscriptionLocalData(
    plrReference = "Abc123",
    subMneOrDomestic = MneOrDomestic.UkAndOther,
    subAccountingPeriod = accountingPeriod,
    subPrimaryContactName = "John",
    subPrimaryEmail = "john@email.com",
    subPrimaryPhonePreference = true,
    subPrimaryCapturePhone = Some("123"),
    subAddSecondaryContact = true,
    subSecondaryContactName = Some("Doe"),
    subSecondaryEmail = Some("doe@email.com"),
    subSecondaryCapturePhone = Some("123"),
    subSecondaryPhonePreference = Some(true),
    subRegisteredAddress = NonUKAddress("line1", None, "line", None, None, "GB"),
    accountStatus = Some(AccountStatus(false))
  )
}
