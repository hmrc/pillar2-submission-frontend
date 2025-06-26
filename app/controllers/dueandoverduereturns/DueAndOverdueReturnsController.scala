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

import cats.data.OptionT
import config.FrontendAppConfig
import controllers.actions._
import controllers.routes.JourneyRecoveryController
import models.UserAnswers
import pages.PlrReferencePage
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.SubscriptionService
import services.obligationsandsubmissions.ObligationsAndSubmissionsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.Constants.SUBMISSION_ACCOUNTING_PERIODS
import views.html.dueandoverduereturns.DueAndOverdueReturnsView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DueAndOverdueReturnsController @Inject() (
  val controllerComponents:         MessagesControllerComponents,
  getData:                          DataRetrievalAction,
  requireData:                      DataRequiredAction,
  obligationsAndSubmissionsService: ObligationsAndSubmissionsService,
  view:                             DueAndOverdueReturnsView,
  subscriptionService:              SubscriptionService,
  sessionRepository:                SessionRepository,
  identify:                         IdentifierAction
)(implicit
  appConfig: FrontendAppConfig,
  ec:        ExecutionContext
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      (for {
        maybeUserAnswer <- OptionT.liftF(sessionRepository.get(request.userId))
        userAnswers = maybeUserAnswer.getOrElse(UserAnswers(request.userId))
        maybeSubscriptionData <- OptionT.liftF(subscriptionService.getSubscriptionCache(request.userId))
        updatedAnswers        <- OptionT.liftF(Future.fromTry(userAnswers.set(PlrReferencePage, maybeSubscriptionData.plrReference)))
        _                     <- OptionT.liftF(sessionRepository.set(updatedAnswers))
        fromDate  = LocalDate.now().minusYears(SUBMISSION_ACCOUNTING_PERIODS)
        toDate    = LocalDate.now()
        pillar2Id = updatedAnswers.get(PlrReferencePage)
        data <- OptionT.liftF(obligationsAndSubmissionsService.handleData(pillar2Id.get, fromDate, toDate))
      } yield Ok(view(data, fromDate, toDate, request.isAgent))).value
        .map(_.getOrElse(Redirect(JourneyRecoveryController.onPageLoad())))
        .recover { case e =>
          logger.error(s"Error calling obligationsAndSubmissionsService.handleData: ${e.getMessage}", e)
          Redirect(JourneyRecoveryController.onPageLoad())
        }
    }
}
