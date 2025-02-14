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

import config.FrontendAppConfig
import models.InternalIssueError
import models.obligation.ObligationInformation
import play.api.Logging
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ObligationConnector @Inject() (val config: FrontendAppConfig, val http: HttpClient) extends Logging {

  @deprecated("Use ObligationsAndSubmissionsConnector instead")
  def getObligation(
    plrReference: String,
    dateFrom:     LocalDate,
    dateTo:       LocalDate
  )(implicit hc:  HeaderCarrier, ec: ExecutionContext): Future[Option[ObligationInformation]] = {
    val url = s"${config.pillar2BaseUrl}/report-pillar2-top-up-taxes/get-obligation/$plrReference/${dateFrom.toString}/${dateTo.toString}"

    http
      .GET[HttpResponse](url)
      .flatMap { response =>
        response.status match {
          case OK =>
            Future.successful(Json.parse(response.body).asOpt[ObligationInformation])
          case NOT_FOUND => Future.successful(None)
          case errorStatus =>
            logger.warn(s"Connection issue when calling get obligation with status: $errorStatus")
            Future.failed(InternalIssueError)
        }
      }

  }
}
