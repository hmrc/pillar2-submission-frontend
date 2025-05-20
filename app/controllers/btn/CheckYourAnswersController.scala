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
import models.btn.{BTNRequest, BTNStatus}
import models.subscription.AccountingPeriod
import models.{InternalIssueError, UserAnswers}
import org.apache.pekko.actor.ActorSystem
import pages._
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import repositories.SessionRepository
import services.BTNService
import services.audit.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.btn.{BTNCannotReturnView, CheckYourAnswersView}

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

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
  auditService:             AuditService,
  actorSystem:              ActorSystem
)(implicit ec:              ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen btnStatus.subscriptionRequest).async { implicit request =>
      sessionRepository.get(request.userId).map { maybeUserAnswers =>
        (for {
          userAnswers   <- maybeUserAnswers
          entitiesInOut <- userAnswers.get(EntitiesInsideOutsideUKPage)
        } yield
          if (entitiesInOut) {
            val summaryList = SummaryListViewModel(
              rows = Seq(
                SubAccountingPeriodSummary.row(request.subscriptionLocalData.subAccountingPeriod),
                BTNEntitiesInsideOutsideUKSummary.row(userAnswers)
              ).flatten
            ).withCssClass("govuk-!-margin-bottom-9")

            Ok(view(summaryList))
          } else {
            Redirect(IndexController.onPageLoad)
          }).getOrElse(Redirect(JourneyRecoveryController.onPageLoad()))
      }
    }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val subAccountingPeriod: AccountingPeriod = request.subscriptionLocalData.subAccountingPeriod
    val btnPayload = BTNRequest(
      accountingPeriodFrom = subAccountingPeriod.startDate,
      accountingPeriodTo = subAccountingPeriod.endDate
    )

    implicit val pillar2Id: String = request.subscriptionLocalData.plrReference

    sessionRepository.get(request.userId).flatMap { maybeUserAnswers =>
      maybeUserAnswers
        .map { userAnswers =>
          val currentStatus = userAnswers.get(BTNStatus)

          if (currentStatus.isEmpty || (currentStatus.get != BTNStatus.processing && currentStatus.get != BTNStatus.submitted)) {

            for {
              processingStatus <- Future.fromTry(request.userAnswers.set(BTNStatus, BTNStatus.processing))
              _                <- sessionRepository.set(processingStatus)

              _ = fireAndForgetSubmission(request.userId, btnPayload, request.userAnswers)
            } yield Redirect(routes.BTNWaitingRoomController.onPageLoad)
          } else {

            logger.info(s"BTN submission already in progress or completed with status: $currentStatus, redirecting to waiting room")
            Future.successful(Redirect(routes.BTNWaitingRoomController.onPageLoad))
          }
        }
        .getOrElse(Future.successful(Redirect(JourneyRecoveryController.onPageLoad())))
    }
  }

  def cannotReturnKnockback: Action[AnyContent] = identify(implicit request => BadRequest(cannotReturnView()))

  private def fireAndForgetSubmission(userId: String, btnPayload: BTNRequest, userAnswers: UserAnswers)(implicit
    pillar2Id:                                String,
    hc:                                       HeaderCarrier
  ): Unit =
    btnService
      .submitBTN(btnPayload)
      .flatMap { apiSuccessResponse =>
        logger.info(s"BTN submission successful, updating status to submitted")

        actorSystem.scheduler.scheduleOnce(1500.milliseconds) {

          sessionRepository.get(userId).flatMap {
            case Some(latestAnswers) =>
              for {
                updatedAnswers <- Future.fromTry(latestAnswers.set(BTNStatus, BTNStatus.submitted))
                _              <- sessionRepository.set(updatedAnswers)
                _ <- auditService.auditBTN(
                       pillarReference = pillar2Id,
                       accountingPeriod = userAnswers.get(SubAccountingPeriodPage).map(_.toString).getOrElse(""),
                       entitiesInsideAndOutsideUK = userAnswers.get(EntitiesInsideOutsideUKPage).getOrElse(false),
                       apiResponseData = ApiResponseData(
                         statusCode = CREATED,
                         processingDate = apiSuccessResponse.processingDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                         errorCode = None,
                         responseMessage = "Success"
                       )
                     )
              } yield logger.info(s"BTN Request Submission was successful. Status updated to submitted.")
            case None =>
              logger.warn("User answers not found when updating status to submitted")
              Future.successful(())
          }
        }

        Future.successful(())
      }
      .recoverWith { case e: Throwable =>
        logger.error(s"BTN Request Submission failed with error: ${e.getMessage}")

        sessionRepository.get(userId).flatMap {
          case Some(latestAnswers) =>
            for {
              errorStatus <- Future.fromTry(latestAnswers.set(BTNStatus, BTNStatus.error))
              _           <- sessionRepository.set(errorStatus)
              _ <- auditService.auditBTN(
                     pillarReference = pillar2Id,
                     accountingPeriod = userAnswers.get(SubAccountingPeriodPage).map(_.toString).getOrElse(""),
                     entitiesInsideAndOutsideUK = userAnswers.get(EntitiesInsideOutsideUKPage).getOrElse(false),
                     apiResponseData = ApiResponseData(
                       statusCode = INTERNAL_SERVER_ERROR,
                       processingDate = java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                       errorCode = Some("InternalIssueError"),
                       responseMessage = e.getMessage
                     )
                   )
            } yield ()
          case None =>
            logger.warn("User answers not found when updating status to error")
            Future.successful(())
        }
      }
      .andThen {
        case scala.util.Success(_) => logger.info("BTN submission completed successfully")
        case scala.util.Failure(e) => logger.error(s"BTN submission failed with: ${e.getMessage}")
      }
}
