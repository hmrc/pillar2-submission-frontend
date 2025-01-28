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

package controllers.btn

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.actions._
import controllers.btn.routes._
import controllers.routes._
import models.btn.BTNStatus
import models.btn.BTNStatus.submitted
import pages._
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.btn.{BTNCannotReturnView, CheckYourAnswersView}

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject() (
  identify:                 IdentifierAction,
  getData:                  DataRetrievalAction,
  getSubscriptionData:      SubscriptionDataRetrievalAction,
  requireData:              DataRequiredAction,
  requireSubscriptionData:  SubscriptionDataRequiredAction,
  btnStatus:                BTNStatusAction,
  sessionRepository:        SessionRepository,
  view:                     CheckYourAnswersView,
  cannotReturnView:         BTNCannotReturnView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec:              ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] =
    (identify andThen getSubscriptionData andThen requireSubscriptionData andThen btnStatus.subscriptionRequest).async { implicit request =>
      sessionRepository.get(request.userId).map { maybeUserAnswers =>
        (for {
          userAnswers    <- maybeUserAnswers
          entitiesInOut  <- userAnswers.get(EntitiesInsideOutsideUKPage)
          last4Periods   <- userAnswers.get(BTNLast4AccountingPeriodsPage)
          nextTwoPeriods <- userAnswers.get(BTNNext2AccountingPeriodsPage)
        } yield (entitiesInOut, last4Periods, nextTwoPeriods) match {
          case (true, false, false) =>
            val summaryList = SummaryListViewModel(
              rows = Seq(
                SubAccountingPeriodSummary.row(request.subscriptionLocalData.subAccountingPeriod),
                BTNEntitiesInsideOutsideUKSummary.row(userAnswers),
                BTNLast4AccountingPeriodSummary.row(userAnswers),
                BTNNext2AccountingPeriodsSummary.row(userAnswers)
              ).flatten
            ).withCssClass("govuk-!-margin-bottom-9")

            Ok(view(summaryList))

          case _ => Redirect(IndexController.onPageLoad)

        }).getOrElse(Redirect(JourneyRecoveryController.onPageLoad()))
      }
    }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(BTNStatus, submitted))
      _              <- sessionRepository.set(updatedAnswers)
    } yield Redirect(BTNConfirmationController.onPageLoad)
  }

  def cannotReturnKnockback: Action[AnyContent] = identify(implicit request => BadRequest(cannotReturnView()))
}
