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

package controllers.btn

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.actions._
import controllers.routes._
import models.audit.ApiResponseData
import models.btn.BTNStatus.submitted
import models.btn.{BTNRequest, BTNStatus}
import models.subscription.AccountingPeriod
import pages._
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.BTNService
import services.audit.AuditService
import uk.gov.hmrc.http.HttpException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.btn.{BTNCannotReturnView, CheckYourAnswersView}

import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject() (
  identify:                 IdentifierAction,
  getData:                  SubscriptionDataRetrievalAction,
  requireData:              SubscriptionDataRequiredAction,
  btnStatus:                BTNStatusAction,
  sessionRepository:        SessionRepository,
  view:                     CheckYourAnswersView,
  cannotReturnView:         BTNCannotReturnView,
  btnService:               BTNService,
  val controllerComponents: MessagesControllerComponents,
  auditService:             AuditService
)(implicit ec:              ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen btnStatus.subscriptionRequest).async { implicit request =>
      sessionRepository.get(request.userId).map { maybeUserAnswers =>
        (for {
          userAnswers    <- maybeUserAnswers
          entitiesInOut  <- userAnswers.get(EntitiesInsideOutsideUKPage)
          last4Periods   <- userAnswers.get(BTNLast4AccountingPeriodsPage)
          nextTwoPeriods <- userAnswers.get(BTNNext2AccountingPeriodsPage)
        } yield {

          userAnswers.get(BTNStatus)

          if (entitiesInOut && !last4Periods && !nextTwoPeriods) {
            val summaryList = SummaryListViewModel(
              rows = Seq(
                SubAccountingPeriodSummary.row(request.subscriptionLocalData.subAccountingPeriod),
                BTNEntitiesInsideOutsideUKSummary.row(userAnswers),
                BTNLast4AccountingPeriodSummary.row(userAnswers),
                BTNNext2AccountingPeriodsSummary.row(userAnswers)
              ).flatten
            ).withCssClass("govuk-!-margin-bottom-9")

            Ok(view(summaryList))
          } else {
            Redirect(IndexController.onPageLoad)
          }
        }).getOrElse {
          Redirect(JourneyRecoveryController.onPageLoad())
        }
      }
    }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val subAccountingPeriod: AccountingPeriod = request.subscriptionLocalData.subAccountingPeriod
    val btnPayload = BTNRequest(
      accountingPeriodFrom = subAccountingPeriod.startDate,
      accountingPeriodTo = subAccountingPeriod.endDate
    )

    implicit val pillar2Id: String = request.subscriptionLocalData.plrReference

    (for {
      apiSuccessResponse <- btnService.submitBTN(btnPayload)
      updatedAnswers     <- Future.fromTry(request.userAnswers.set(BTNStatus, submitted))
      _                  <- sessionRepository.set(updatedAnswers)

      _ <- auditService.auditBTN(
             pillarReference = request.subscriptionLocalData.plrReference,
             accountingPeriod = subAccountingPeriod.toString,
             entitiesInsideAndOutsideUK = request.userAnswers.get(EntitiesInsideOutsideUKPage).getOrElse(false),
             apiResponseData = ApiResponseData(
               statusCode = CREATED,
               processingDate = apiSuccessResponse.processingDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
               errorCode = None,
               responseMessage = "Success"
             )
           )

      _ = logger.info(s"BTN Request Submission was successful. response.body= $apiSuccessResponse")

    } yield Redirect(controllers.btn.routes.BTNConfirmationController.onPageLoad)).recoverWith { case e: HttpException =>
      logger.error(s"BTN Request Submission failed with status ${e.responseCode}: ${e.message}")

      for {
        _ <- auditService.auditBTN(
               pillarReference = request.subscriptionLocalData.plrReference,
               accountingPeriod = subAccountingPeriod.toString,
               entitiesInsideAndOutsideUK = request.userAnswers.get(EntitiesInsideOutsideUKPage).getOrElse(false),
               apiResponseData = ApiResponseData(
                 statusCode = e.responseCode,
                 processingDate = java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                 errorCode = Some("InternalIssueError"),
                 responseMessage = e.message
               )
             )
      } yield Redirect(controllers.btn.routes.BTNProblemWithServiceController.onPageLoad)
    }
  }

  def cannotReturnKnockback: Action[AnyContent] = identify(implicit request => BadRequest(cannotReturnView()))
}
