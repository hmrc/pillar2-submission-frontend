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

import models.subscription._
import models.{MneOrDomestic, NonUKAddress}

import java.time.LocalDate

trait SubscriptionLocalDataFixture {
  private val upeCorrespondenceAddress = UpeCorrespAddressDetails("middle", None, Some("lane"), None, None, "obv")
  private val contactDetails           = ContactDetailsType("shadow", Some("dota2"), "shadow@fiend.com")
  private lazy val currentDate: LocalDate = LocalDate.now()

  val subscriptionData: SubscriptionData = SubscriptionData(
    formBundleNumber = "form bundle",
    upeDetails = UpeDetails(None, None, None, "orgName", LocalDate.of(2024, 1, 31), domesticOnly = false, filingMember = false),
    upeCorrespAddressDetails = upeCorrespondenceAddress,
    primaryContactDetails = contactDetails,
    secondaryContactDetails = None,
    filingMemberDetails = None,
    accountingPeriod = AccountingPeriod(currentDate, currentDate.plusYears(1)),
    accountStatus = Some(AccountStatus(false))
  )

  val someSubscriptionLocalData: SubscriptionLocalData = SubscriptionLocalData(
    plrReference = "Abc123",
    subMneOrDomestic = MneOrDomestic.Uk,
    subAccountingPeriod = AccountingPeriod(LocalDate.now, LocalDate.now.plusYears(1)),
    subPrimaryContactName = "John",
    subPrimaryEmail = "john@email.com",
    subPrimaryPhonePreference = true,
    subPrimaryCapturePhone = Some("123"),
    subAddSecondaryContact = true,
    subSecondaryContactName = Some("Doe"),
    subSecondaryEmail = Some("doe@email.com"),
    subSecondaryCapturePhone = Some("123"),
    subSecondaryPhonePreference = Some(true),
    subRegisteredAddress = NonUKAddress("line1", None, "line", None, None, "GB")
  )

  val someSubscriptionLocalDataUkOther: SubscriptionLocalData = SubscriptionLocalData(
    plrReference = "Abc123",
    subMneOrDomestic = MneOrDomestic.UkAndOther,
    subAccountingPeriod = AccountingPeriod(LocalDate.now, LocalDate.now.plusYears(1)),
    subPrimaryContactName = "John",
    subPrimaryEmail = "john@email.com",
    subPrimaryPhonePreference = true,
    subPrimaryCapturePhone = Some("123"),
    subAddSecondaryContact = true,
    subSecondaryContactName = Some("Doe"),
    subSecondaryEmail = Some("doe@email.com"),
    subSecondaryCapturePhone = Some("123"),
    subSecondaryPhonePreference = Some(true),
    subRegisteredAddress = NonUKAddress("line1", None, "line", None, None, "GB")
  )

}
