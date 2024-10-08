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
import connectors.ObligationConnectorSpec._
import models.InternalIssueError
import models.obligation.ObligationInformation
import models.obligation.ObligationStatus.Fulfilled
import models.obligation.ObligationType.UKTR
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json

import java.time.LocalDate

class ObligationConnectorSpec extends SpecBase with WireMockServerHandler {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = "microservice.services.pillar2.port" -> server.port()
    )
    .build()

  lazy val connector: ObligationConnector = app.injector.instanceOf[ObligationConnector]

  private val dateFrom                  = LocalDate.now
  private val dateTo                    = LocalDate.now.plusYears(1)
  private val obligationInformationJson = Json.toJson(obligationInformation)

  "get obligation" should {

    "return obligation information when the backend has returned 200 OK with data" in {
      stubGet(s"$ReadSubscriptionPath/$PlrReference/$dateFrom/$dateTo", OK, obligationInformationJson.toString())
      val result: Option[ObligationInformation] = connector.getObligation(PlrReference, dateFrom, dateTo).futureValue

      result mustBe defined
      result mustBe Some(obligationInformation)

    }

    "return no information error when the backend has returned a 404 status" in {
      stubGet(s"$ReadSubscriptionPath/$PlrReference/$dateFrom/$dateTo", NOT_FOUND, unsuccessfulNotFoundJson)
      val result = connector.getObligation(PlrReference, dateFrom, dateTo).futureValue
      result mustBe None
    }

    "return None when the backend has returned a response else than 200 or 404 status" in {
      stubGet(s"$ReadSubscriptionPath/$PlrReference/$dateFrom/$dateTo", errorCodes.sample.value, unsuccessfulResponseJson)
      val result = connector.getObligation(PlrReference, dateFrom, dateTo)
      result.failed.futureValue mustBe InternalIssueError
    }
  }
}

object ObligationConnectorSpec {
  val PlrReference         = "XEPLR0000000000"
  val ReadSubscriptionPath = "/report-pillar2-top-up-taxes/get-obligation"
  val obligationInformation = ObligationInformation(
    obligationType = UKTR,
    status = Fulfilled,
    accountingPeriodFromDate = LocalDate.now.minusYears(1),
    accountingPeriodToDate = LocalDate.now,
    dueDate = LocalDate.now.plusMonths(10)
  )

  private val unsuccessfulNotFoundJson =
    """{ "status": "404",
      | "error": "there is nothing here" }""".stripMargin
  private val unsuccessfulResponseJson = """{ "status": "error" }"""
}
