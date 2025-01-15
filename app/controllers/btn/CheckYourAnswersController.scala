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
import controllers.routes._
import models.btn.BtnRequest
import models.subscription.AccountingPeriod
import pages.{BtnRevenues750In2AccountingPeriodPage, BtnRevenues750InNext2AccountingPeriodsPage, EntitiesBothInUKAndOutsidePage}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.BtnService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.btn.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject() (
  identify:                 IdentifierAction,
  getData:                  SubscriptionDataRetrievalAction,
  requireData:              SubscriptionDataRequiredAction,
  sessionRepository:        SessionRepository,
  view:                     CheckYourAnswersView,
  val controllerComponents: MessagesControllerComponents,
  btnService:               BtnService
)(implicit ec:              ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    sessionRepository.get(request.userId).map { maybeUserAnswers =>
      (for {
        userAnswers                   <- maybeUserAnswers
        entitiesInOutPage             <- userAnswers.get(EntitiesBothInUKAndOutsidePage)
        revenuePreviousTwoPeriodsPage <- userAnswers.get(BtnRevenues750In2AccountingPeriodPage)
        revenueNextTwoPeriodsPage     <- userAnswers.get(BtnRevenues750InNext2AccountingPeriodsPage)
      } yield (entitiesInOutPage, revenuePreviousTwoPeriodsPage, revenueNextTwoPeriodsPage) match {
        case (true, false, false) =>
          val summaryList = SummaryListViewModel(
            rows = Seq(
              SubAccountingPeriodSummary.row(request.subscriptionLocalData.subAccountingPeriod),
              BtnEntitiesBothInUKAndOutsideSummary.row(userAnswers),
              BtnRevenues750In2AccountingPeriodSummary.row(userAnswers),
              BtnRevenues750InNext2AccountingPeriodsSummary.row(userAnswers)
            ).flatten
          ).withCssClass("govuk-!-margin-bottom-9")

          Ok(view(summaryList))

        case _ => Redirect(IndexController.onPageLoad)

      }).getOrElse(Redirect(JourneyRecoveryController.onPageLoad()))
    }
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val subAccountingPeriod: AccountingPeriod = request.subscriptionLocalData.subAccountingPeriod
    val btnPayload = BtnRequest(
      accountingPeriodFrom = subAccountingPeriod.startDate,
      accountingPeriodTo = subAccountingPeriod.endDate
    )

    val btnServiceResult: Future[HttpResponse] =
      btnService.submitBtn(btnPayload, request.subscriptionLocalData.plrReference)

    btnServiceResult.map {
      case httpResponse: HttpResponse =>
        if (httpResponse.status == CREATED) {
          logger.info(
            s"Btn Request Submission was successful: httpResponse status= ${httpResponse.status}"
              + " httpResponse.body=" + httpResponse.body
          )
          Redirect(controllers.btn.routes.BtnConfirmationController.onPageLoad)
        } else {
          logger.debug(
            s"Btn Request failed with invalid httpResponse.status: ${httpResponse.status}"
              + " httpResponse.body=" + httpResponse.body
          )
          Redirect(controllers.routes.UnderConstructionController.onPageLoad)
        }
      case exception =>
        logger.debug(s"Btn Request failed with exception: $exception")
        Redirect(controllers.routes.UnderConstructionController.onPageLoad)
    }
  }
}
