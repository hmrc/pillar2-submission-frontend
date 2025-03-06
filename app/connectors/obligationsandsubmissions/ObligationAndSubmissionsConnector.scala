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

package connectors.obligationsandsubmissions

import config.FrontendAppConfig
import models.obligationsandsubmissions._
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ObligationAndSubmissionsConnector @Inject() (val config: FrontendAppConfig, val http: HttpClientV2) extends Logging {

  def getData(dateFrom: LocalDate, dateTo: LocalDate)(implicit
    hc:                 HeaderCarrier,
    ec:                 ExecutionContext,
    pillar2Id:          String
  ): Future[ObligationsAndSubmissionsSuccess] = {
    val url =
      s"${config.pillar2BaseUrl}/report-pillar2-top-up-taxes/obligations-and-submissions/$dateFrom/$dateTo"
    logger.info(s"Calling the backend ($url) with pillar2Id: $pillar2Id")

    http
      .get(url"$url")
      .setHeader("X-Pillar2-Id" -> pillar2Id)
      .execute[HttpResponse]
      .flatMap(response => Future.successful(Json.parse(response.body).as[ObligationsAndSubmissionsSuccess]))
  }
}
