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
    val submissionTimestamp = System.currentTimeMillis().toString

    // Immediately set status to processing and redirect (this is what we wait for)
    val redirectFuture = for {
      processingStatus <- Future.fromTry(request.userAnswers.set(BTNStatus, BTNStatus.processing))
      _                <- sessionRepository.set(processingStatus)
    } yield Redirect(routes.BTNWaitingRoomController.onPageLoad)
      .removingFromSession("btn_submission_initiated") // Clear existing marker first
      .addingToSession("btn_submission_initiated" -> "true")
      .addingToSession("btn_submission_timestamp" -> submissionTimestamp)

    // Start the actual submission in parallel - this runs completely independently
    // and has no dependency on the redirect future completing
    val submissionFuture = btnService
      .submitBTN(btnPayload)
      .flatMap { apiSuccessResponse =>
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(BTNStatus, submitted))
          _              <- sessionRepository.set(updatedAnswers)
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
        } yield logger.info(s"BTN Request Submission was successful. response.body= $apiSuccessResponse")
      }
      .recoverWith { case e: Throwable =>
        logger.error(s"BTN Request Submission failed with error: ${e.getMessage}")

        for {
          errorStatus <- Future.fromTry(request.userAnswers.set(BTNStatus, BTNStatus.error))
          _           <- sessionRepository.set(errorStatus)
          _ <- auditService.auditBTN(
                 pillarReference = request.subscriptionLocalData.plrReference,
                 accountingPeriod = subAccountingPeriod.toString,
                 entitiesInsideAndOutsideUK = request.userAnswers.get(EntitiesInsideOutsideUKPage).getOrElse(false),
                 apiResponseData = ApiResponseData(
                   statusCode = INTERNAL_SERVER_ERROR,
                   processingDate = java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                   errorCode = Some("InternalIssueError"),
                   responseMessage = e.getMessage
                 )
               )
        } yield ()
      }

    // Make sure the submission future is properly registered in the execution context
    // This ensures it will run even though we're not waiting for the result
    submissionFuture.andThen {
      case scala.util.Success(_) => logger.info("BTN submission completed successfully")
      case scala.util.Failure(e) => logger.error(s"BTN submission failed with: ${e.getMessage}")
    }

    // Only return the redirect future - this completes quickly, showing the spinner
    redirectFuture
  }

  def cannotReturnKnockback: Action[AnyContent] = identify(implicit request => BadRequest(cannotReturnView()))
}
