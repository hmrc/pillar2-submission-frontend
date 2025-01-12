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
import models.btn.{BTNRequest, BTNRequestParameters}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, ZoneOffset, ZonedDateTime}

class BTNConnectorSpec extends SpecBase with WireMockServerHandler {
  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(conf = "microservice.services.pillar2.port" -> server.port())
    .build()
  lazy val connector: BTNConnector = app.injector.instanceOf[BTNConnector]
  val submitBTNPath = "/report-pillar2-top-up-taxes/below-threshold-notification/submit"
  val btnRequestBodyDefaultAccountingPeriodDates: BTNRequest = BTNRequest(
    accountingPeriodFrom = LocalDate.now.minusYears(1),
    accountingPeriodTo = LocalDate.now
  )
  val stubProcessedZonedDateTime: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS)
  val defaultSuccessfulBtnResponseBodyJsObject = Json.obj(
    "processingDate"   -> stubProcessedZonedDateTime,
    "formBundleNumber" -> "11223344556677",
    "chargeReference"  -> "XTC01234123412"
  )

  "submit BTN connector " should {
    "return 201 CREATED when the pillar-2 backend submitBTN  call has returned 201 CREATED." in {
      val plrReferenceForValidResponse = "XEPLR0000000000"
      val accountingPeriodFromDateMinus1Year: LocalDate = LocalDate.now.minusYears(1)
      val accountingPeriodToDateNow = LocalDate.now
      val btnRequestDatesMinus1YearAndNow: BTNRequest = BTNRequest(accountingPeriodFromDateMinus1Year, accountingPeriodToDateNow)

      stubResponse(submitBTNPath, CREATED, defaultSuccessfulBtnResponseBodyJsObject.toString())

      val result: HttpResponse = connector.submitBTN(btnRequestDatesMinus1YearAndNow: BTNRequest, plrReferenceForValidResponse).futureValue
      result.status mustBe CREATED
      result.json mustBe defaultSuccessfulBtnResponseBodyJsObject
    }
  }
}

object BTNConnectorSpec {
  val plrReference = "XEPLR0000000000"
  val accountingPeriodFromDate: LocalDate = LocalDate.now.minusYears(1)
  val accountingPeriodToDate = LocalDate.now
  val btnRequest: BTNRequest = BTNRequest(accountingPeriodFromDate, accountingPeriodToDate)
  val btnRequestParameters: BTNRequestParameters = BTNRequestParameters(
    plrReference,
    accountingPeriodFromDate,
    accountingPeriodToDate
  )
  val btnValidRequest: BTNRequestParameters = BTNRequestParameters(
    "XEPLR0000000000",
    LocalDate.now.minusYears(1),
    LocalDate.now
  )
  val btnInvalidBadRequest400: BTNRequestParameters = BTNRequestParameters(
    "XEPLR4000000000",
    LocalDate.now.minusYears(1),
    LocalDate.now
  )
  val btnUnprocessableEntityRequest422: BTNRequestParameters = BTNRequestParameters(
    "XEPLR4220000000",
    LocalDate.now.minusYears(1),
    LocalDate.now
  )
  val btnInternalServerErrorRequest500: BTNRequestParameters = BTNRequestParameters(
    "XEPLR5000000000",
    LocalDate.now.minusYears(1),
    LocalDate.now
  )
}
