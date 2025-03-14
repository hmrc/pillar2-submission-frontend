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

package controllers.dueandoverduereturns

import config.FrontendAppConfig
import controllers.actions._
import controllers.routes.JourneyRecoveryController
import models.requests.SubscriptionDataRequest
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.obligationsandsubmissions.ObligationsAndSubmissionsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.Constants.SUBMISSION_ACCOUNTING_PERIODS
import views.html.dueandoverduereturns.DueAndOverdueReturnsView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class DueAndOverdueReturnsController @Inject() (
  val controllerComponents:               MessagesControllerComponents,
  getSubscriptionData:                    SubscriptionDataRetrievalAction,
  requireSubscriptionData:                SubscriptionDataRequiredAction,
  obligationsAndSubmissionsService:       ObligationsAndSubmissionsService,
  view:                                   DueAndOverdueReturnsView,
  @Named("EnrolmentIdentifier") identify: IdentifierAction
)(implicit
  appConfig: FrontendAppConfig,
  ec:        ExecutionContext
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getSubscriptionData andThen requireSubscriptionData).async { implicit request: SubscriptionDataRequest[AnyContent] =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val fromDate  = LocalDate.now().minusYears(SUBMISSION_ACCOUNTING_PERIODS)
      val toDate    = LocalDate.now()
      val pillar2Id = request.subscriptionLocalData.plrReference
      obligationsAndSubmissionsService
        .handleData(pillar2Id, fromDate, toDate)
        .map { data =>
          Ok(view(data, fromDate, toDate, request.isAgent))

        }
        .recover { case e =>
          logger.error(s"Error calling obligationsAndSubmissionsService.handleData: ${e.getMessage}", e)
          Redirect(JourneyRecoveryController.onPageLoad())
        }
    }
}
