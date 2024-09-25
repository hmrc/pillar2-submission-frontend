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
import models.subscription.{SubscriptionData, SubscriptionLocalData, SubscriptionSuccess}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionConnector @Inject() (val config: FrontendAppConfig, val http: HttpClient) extends Logging {
  def readSubscription(
    plrReference: String
  )(implicit hc:  HeaderCarrier, ec: ExecutionContext): Future[Option[SubscriptionData]] = {
    val subscriptionUrl = s"${config.pillar2BaseUrl}/report-pillar2-top-up-taxes/subscription/read-subscription/$plrReference"

    http
      .GET[HttpResponse](subscriptionUrl)
      .flatMap {
        case response if response.status == 200 =>
          Future.successful(Some(Json.parse(response.body).as[SubscriptionSuccess].success))
        case notFoundResponse if notFoundResponse.status == 404 => Future.successful(None)
        case e =>
          logger.warn(s"Connection issue when calling read subscription with status: ${e.status}")
          Future.failed(InternalIssueError)
      }
  }

  def getSubscriptionCache(
    userId:      String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SubscriptionLocalData]] =
    http
      .GET[HttpResponse](s"${config.pillar2BaseUrl}/report-pillar2-top-up-taxes/user-cache/read-subscription/$userId")
      .map {
        case response if response.status == 200 =>
          Some(Json.parse(response.body).as[SubscriptionLocalData])
        case e =>
          logger.warn(s"Connection issue when calling read subscription with status: ${e.status} ${e.body}")
          None
      }

}
