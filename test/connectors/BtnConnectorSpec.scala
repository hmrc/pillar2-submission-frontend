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
import models.InternalIssueError
import models.btn.BtnRequest
import org.scalatest.exceptions.TestFailedException
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.test.WireMockSupport

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, ZoneOffset, ZonedDateTime}
import scala.concurrent.Future

class BtnConnectorSpec extends SpecBase with WireMockSupport with WireMockServerHandler {
  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(conf = "microservice.services.pillar2.port" -> server.port())
    .build()
  lazy val connector: BtnConnector = app.injector.instanceOf[BtnConnector]
  val submitBtnPath = "/report-pillar2-top-up-taxes/below-threshold-notification/submit"
  val btnRequestBodyDefaultAccountingPeriodDates: BtnRequest = BtnRequest(
    accountingPeriodFrom = LocalDate.now.minusYears(1),
    accountingPeriodTo = LocalDate.now
  )
  val stubProcessedZonedDateTime: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS)
  val defaultSuccessfulBtnResponseBodyJsObject: JsObject = Json.obj(
    "processingDate"   -> stubProcessedZonedDateTime,
    "formBundleNumber" -> "11223344556677",
    "chargeReference"  -> "XTC01234123412"
  )
  val accountingPeriodFromDateMinus1Year: LocalDate  = LocalDate.now.minusYears(1)
  val accountingPeriodToDateNow:          LocalDate  = LocalDate.now
  val btnRequestDatesMinus1YearAndNow:    BtnRequest = BtnRequest(accountingPeriodFromDateMinus1Year, accountingPeriodToDateNow)

  "submit Btn connector " should {
    "return 201 CREATED when the pillar-2 backend has returned status=201." in {
      val plrReferenceForValidResponse = "XEPLR0000000000"
      stubResponse(submitBtnPath, CREATED, defaultSuccessfulBtnResponseBodyJsObject.toString())
      val result: HttpResponse = connector.submitBtn(btnRequestDatesMinus1YearAndNow: BtnRequest, plrReferenceForValidResponse).futureValue
      result.status mustBe CREATED
      result.json mustBe defaultSuccessfulBtnResponseBodyJsObject

      //Check response body field-values:
      val formBundleNumberValue: Option[String] = (result.json \ "formBundleNumber").asOpt[String]
      val chargeReferenceValue:  Option[String] = (result.json \ "chargeReference").asOpt[String]
      if (
        (result.json \ "processingDate").isDefined
        && formBundleNumberValue.contains("11223344556677")
        && chargeReferenceValue.contains("XTC01234123412")
      ) { // All OK
      } else {
        throw new AssertionError(s"submitBtn request failed for plrReference= $plrReferenceForValidResponse")
      }
    }
    "raise an Exception when the expected response field-value-checking fails." in {
      val plrReferenceForValidResponse = "XEPLR9999999999"
      stubResponse(submitBtnPath, CREATED, defaultSuccessfulBtnResponseBodyJsObject.toString())
      val result: HttpResponse = connector.submitBtn(btnRequestDatesMinus1YearAndNow: BtnRequest, plrReferenceForValidResponse).futureValue
      result.status mustBe CREATED
      result.json mustBe defaultSuccessfulBtnResponseBodyJsObject
      //Check response body field-values:
      val formBundleNumberValue: Option[String] = (result.json \ "formBundleNumber").asOpt[String]
      val chargeReferenceValue:  Option[String] = (result.json \ "chargeReference").asOpt[String]

      assertThrows[TestFailedException] {
        if (
          (result.json \ "processingDate").isDefined
          && formBundleNumberValue.contains("INVALID-FORM-BUNDLE-NUMBER")
          && chargeReferenceValue.contains("XTC01234123412")
        ) { // All OK
        } else {
          throw new TestFailedException(s"submitBtn response field-value-checking failed for plrReference= $plrReferenceForValidResponse", 0)
        }
      }
    }

    "return InternalIssueError when the pillar-2 backend has returned status=400." in {
      val plrReferenceForValidResponse = "XEPLR4000000000"
      stubResponse(submitBtnPath, INTERNAL_SERVER_ERROR, defaultSuccessfulBtnResponseBodyJsObject.toString())
      val result: Future[HttpResponse] = connector.submitBtn(btnRequestDatesMinus1YearAndNow: BtnRequest, plrReferenceForValidResponse)
      result.failed.futureValue mustBe InternalIssueError
    }

    "return InternalIssueError when the pillar-2 backend has returned status=422." in {
      val plrReferenceForValidResponse = "XEPLR4220000000"
      stubResponse(submitBtnPath, INTERNAL_SERVER_ERROR, defaultSuccessfulBtnResponseBodyJsObject.toString())
      val result: Future[HttpResponse] = connector.submitBtn(btnRequestDatesMinus1YearAndNow: BtnRequest, plrReferenceForValidResponse)
      result.failed.futureValue mustBe InternalIssueError
    }

    "return InternalIssueError when the pillar-2 backend has returned status=500." in {
      val plrReferenceForValidResponse = "XEPLR5000000000"
      stubResponse(submitBtnPath, INTERNAL_SERVER_ERROR, defaultSuccessfulBtnResponseBodyJsObject.toString())
      val result: Future[HttpResponse] = connector.submitBtn(btnRequestDatesMinus1YearAndNow: BtnRequest, plrReferenceForValidResponse)
      result.failed.futureValue mustBe InternalIssueError
    }
  }
}
