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
import models.MneOrDomestic.Uk
import models.audit.ApiResponseData
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
            val multipleAccountingPeriods = request.userAnswers.get(BTNChooseAccountingPeriodPage).isDefined
            val summaryList = SummaryListViewModel(
              rows = Seq(
                SubAccountingPeriodSummary.row(request.subscriptionLocalData.subAccountingPeriod, multipleAccountingPeriods),
                BTNEntitiesInsideOutsideUKSummary.row(userAnswers, request.subscriptionLocalData.subMneOrDomestic == Uk)
              ).flatten
            ).withCssClass("govuk-!-margin-bottom-9")

            Ok(view(summaryList))
          } else {
            Redirect(IndexController.onPageLoad)
          }).getOrElse(Redirect(controllers.btn.routes.BTNProblemWithServiceController.onPageLoad))
      }
    }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val subAccountingPeriod: AccountingPeriod =
      request.subscriptionLocalData.subAccountingPeriod //TODO: New logic needed for accounting period details
    val btnPayload = BTNRequest(
      accountingPeriodFrom = subAccountingPeriod.startDate,
      accountingPeriodTo = subAccountingPeriod.endDate
    )

    implicit val pillar2Id: String = request.subscriptionLocalData.plrReference

    val setProcessingF: Future[Unit] = for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(BTNStatus, BTNStatus.processing))
      _              <- sessionRepository.set(updatedAnswers)
    } yield ()

    setProcessingF.foreach { _ =>
      btnService
        .submitBTN(btnPayload)
        .flatMap { resp =>
          sessionRepository.get(request.userId).flatMap {
            case Some(latest) =>
              for {
                submittedAnswers <- Future.fromTry(latest.set(BTNStatus, BTNStatus.submitted))
                _                <- sessionRepository.set(submittedAnswers)
                _ <- auditService.auditBTN(
                       pillarReference = pillar2Id,
                       accountingPeriod = subAccountingPeriod.toString,
                       entitiesInsideAndOutsideUK = request.userAnswers.get(EntitiesInsideOutsideUKPage).getOrElse(false),
                       apiResponseData = ApiResponseData(
                         statusCode = CREATED,
                         processingDate = resp.processingDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                         errorCode = None,
                         responseMessage = "Success"
                       )
                     )
              } yield ()
            case None =>
              Future.successful(())
          }
        }
        .recover { case err =>
          sessionRepository.get(request.userId).flatMap {
            case Some(latest) =>
              for {
                errorAnswers <- Future.fromTry(latest.set(BTNStatus, BTNStatus.error))
                _            <- sessionRepository.set(errorAnswers)
                _ <- auditService.auditBTN(
                       pillarReference = pillar2Id,
                       accountingPeriod = subAccountingPeriod.toString,
                       entitiesInsideAndOutsideUK = request.userAnswers.get(EntitiesInsideOutsideUKPage).getOrElse(false),
                       apiResponseData = ApiResponseData(
                         statusCode = INTERNAL_SERVER_ERROR,
                         processingDate = java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                         errorCode = Some("InternalIssueError"),
                         responseMessage = err.getMessage
                       )
                     )
              } yield ()
            case None =>
              Future.successful(())
          }
        }
    }

    Future.successful(Redirect(routes.BTNWaitingRoomController.onPageLoad))
  }

  def cannotReturnKnockback: Action[AnyContent] =
    identify(implicit request => BadRequest(cannotReturnView()))
}
