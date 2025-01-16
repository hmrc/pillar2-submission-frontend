/*
 * Copyright 2025 HM Revenue & Customs
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

import config.FrontendAppConfig
import models.InternalIssueError
import models.btn.{ApiSuccessResponse, BTNRequest, BTNSuccessResponse, ProcessingDate}
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.time.ZonedDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BTNConnector @Inject() (val config: FrontendAppConfig, val httpClientV2: HttpClientV2) extends Logging {
  def reCastHttpResponse(response: HttpResponse): Future[BTNSuccessResponse] = {
    // Extract the JSON body from the HttpResponse
    val jsonBody = response.json

    // Parse the JSON into an instance of ApiSuccessResponse
    Future.successful(jsonBody.as[BTNSuccessResponse])
  }

  def submitBTN(btnRequest: BTNRequest)(implicit hc: HeaderCarrier, pillar2Id: String, ec: ExecutionContext): Future[HttpResponse] = {
    val urlBTN = s"${config.pillar2BaseUrl}/report-pillar2-top-up-taxes/below-threshold-notification/submit"
    logger.info(s"Calling pillar-2 backend url = $urlBTN with pillar2Id: $pillar2Id.")
    httpClientV2
      .post(url"$urlBTN")
      .withBody(Json.toJson(btnRequest))
      .setHeader("X-Pillar2-Id" -> pillar2Id)
      .execute[HttpResponse]
      .flatMap { response: HttpResponse =>
        response.status match {
          case CREATED =>
            logger.info(s"submitBTN request successful with status = ${response.status}. HttpResponse = $response. ")
            val procDatJSONObj: JsObject = Json.obj("processingDate"   -> Json.parse(response.body).apply("processingDate"))
            println("procDatJSONObj=" + procDatJSONObj.toString() )
            val jsonBody: JsValue = response.json
            val modifiedJson: JsValue = jsonBody.as[JsObject] - "formBundleNumber" - "chargeReference"
            // this is: {"processingDate":"2025-01-16T18:34:47Z"}
            // now add the success wrapper:
//            val processingDate : OFormat[ProcessingDate]  = ProcessingDate.format
//            val successResponse = BTNSuccessResponse())

            val successJSONObj: JsObject = Json.obj("success" -> modifiedJson)

            val btnHttpResponse = new HttpResponse {
              override def status  = response.status
              override def body    = successJSONObj.toString()
              override def headers = response.headers
            }
            println("btnHttpResponse=" + btnHttpResponse.toString() )
            println("btnHttpResponse.body=" + btnHttpResponse.body )

            Future.successful(btnHttpResponse)
          case _ =>
            logger.warn(
              s"submitBTN failed with status = ${response.status} "
                + s" for pillar2Id $pillar2Id"
                + s" and (accountingPeriodFrom, To) = $btnRequest."
            )
            Future.failed(InternalIssueError)
        }
      }
  }
}
