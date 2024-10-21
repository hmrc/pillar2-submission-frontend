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

package connectors

import base.{SpecBase, WireMockServerHandler}
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.SubscriptionConnectorSpec._
import models.subscription._
import models.{InternalIssueError, MneOrDomestic, NonUKAddress}
import org.scalacheck.Gen
import models.{InternalIssueError, MneOrDomestic, NonUKAddress}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

class SubscriptionConnectorSpec extends SpecBase with WireMockServerHandler {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = "microservice.services.pillar2.port" -> server.port()
    )
    .build()

  lazy val connector: SubscriptionConnector = app.injector.instanceOf[SubscriptionConnector]
  private val subscriptionDataJson = Json.parse(successfulResponseJson).as[SubscriptionData]
  val subscriptionSuccess: JsValue = Json.toJson(SubscriptionSuccess(subscriptionDataJson))

  "SubscriptionConnector" must {

    "readSubscription" should {

      "return Some(json) when the backend has returned 200 OK with data" in {
        stubGet(s"$readSubscriptionPath/$plrReference", OK, subscriptionSuccess.toString)
        val result: Option[SubscriptionData] = connector.readSubscription(plrReference).futureValue

        result mustBe defined
        result mustBe Some(subscriptionDataJson)

      }

      "return NoResult error when the backend has returned a 404 status" in {
        stubGet(s"$readSubscriptionPath/$plrReference", NOT_FOUND, unsuccessfulNotFoundJson)
        val result = connector.readSubscription(plrReference).futureValue
        result mustBe None
      }

      "return None when the backend has returned a response else than 200 or 404 status" in {
        stubGet(s"$readSubscriptionPath/$plrReference", errorCodes.sample.value, unsuccessfulResponseJson)
        val result = connector.readSubscription(plrReference)
        result.failed.futureValue mustBe InternalIssueError
      }
    }

    "getSubscriptionCache" should {
      val emptySubscriptionLocalData: SubscriptionLocalData = SubscriptionLocalData(
        plrReference = plrReference,
        subMneOrDomestic = MneOrDomestic.Uk,
        subAccountingPeriod = AccountingPeriod(LocalDate.now, LocalDate.now.plusYears(1)),
        subPrimaryContactName = "",
        subPrimaryEmail = "",
        subPrimaryPhonePreference = false,
        subPrimaryCapturePhone = None,
        subAddSecondaryContact = false,
        subSecondaryContactName = None,
        subSecondaryEmail = None,
        subSecondaryCapturePhone = None,
        subSecondaryPhonePreference = Some(false),
        subRegisteredAddress = NonUKAddress("", None, "", None, None, ""),
        accountStatus = Some(AccountStatus(false))
      )

      "return Some(json) when the backend has returned 200 OK with data" in {
        stubGet(s"$getSubscription/$id", OK, Json.toJson(emptySubscriptionLocalData).toString)
        val result: Option[SubscriptionLocalData] = connector.getSubscriptionCache(id).futureValue

        result mustBe defined
        result mustBe Some(emptySubscriptionLocalData)

      }

      "return None when the backend has returned a non-success status code" in {
        server.stubFor(
          get(urlEqualTo(s"$getSubscription/$id"))
            .willReturn(aResponse().withStatus(errorCodes.sample.value))
        )

        val result = connector.getSubscriptionCache(id).futureValue
        result mustBe None
      }
    }
  }

}

object SubscriptionConnectorSpec {
  val apiUrl                       = "/report-pillar2-top-up-taxes"
  private val getSubscription      = "/report-pillar2-top-up-taxes/user-cache/read-subscription"
  private val readSubscriptionPath = "/report-pillar2-top-up-taxes/subscription/read-subscription"
  private val id                   = "testId"
  private val plrReference         = "testPlrRef"
  private val successfulResponseJson =
    """
      |{
      |
      |      "formBundleNumber": "119000004320",
      |      "upeDetails": {
      |          "domesticOnly": false,
      |          "organisationName": "International Organisation Inc.",
      |          "customerIdentification1": "12345678",
      |          "customerIdentification2": "12345678",
      |          "registrationDate": "2022-01-31",
      |          "filingMember": false
      |      },
      |      "upeCorrespAddressDetails": {
      |          "addressLine1": "1 High Street",
      |          "addressLine2": "Egham",
      |
      |          "addressLine3": "Wycombe",
      |          "addressLine4": "Surrey",
      |          "postCode": "HP13 6TT",
      |          "countryCode": "GB"
      |      },
      |      "primaryContactDetails": {
      |          "name": "Fred Flintstone",
      |          "telephone": "0115 9700 700",
      |          "emailAddress": "fred.flintstone@aol.com"
      |      },
      |      "secondaryContactDetails": {
      |          "name": "Donald Trump",
      |          "telephone": "0115 9700 701",
      |          "emailAddress": "donald.trump@potus.com"
      |
      |      },
      |      "filingMemberDetails": {
      |          "safeId": "XL6967739016188",
      |          "organisationName": "Domestic Operations Ltd",
      |          "customerIdentification1": "1234Z678",
      |          "customerIdentification2": "1234567Y"
      |      },
      |      "accountingPeriod": {
      |          "startDate": "2024-01-06",
      |          "endDate": "2025-04-06",
      |          "duetDate": "2024-04-06"
      |      },
      |      "accountStatus": {
      |          "inactive": true
      |      }
      |  }
      |""".stripMargin

  private val unsuccessfulResponseJson = """{ "status": "error" }"""
  private val unsuccessfulNotFoundJson =
    """{ "status": "404",
      | "error": "there is nothing here" }""".stripMargin
}
