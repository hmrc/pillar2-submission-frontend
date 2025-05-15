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

import config.FrontendAppConfig
import controllers.actions.{IdentifierAction, SubscriptionDataRequiredAction, SubscriptionDataRetrievalAction}
import models.btn.BTNStatus
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.btn.BTNWaitingRoomView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BTNWaitingRoomController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify:                 IdentifierAction,
  getData:                  SubscriptionDataRetrievalAction,
  requireData:              SubscriptionDataRequiredAction,
  view:                     BTNWaitingRoomView
)(implicit ec:              ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val status              = request.userAnswers.get(BTNStatus)
    val submissionInitiated = request.session.get("btn_submission_initiated").isDefined
    val submissionTimestamp = request.session.get("btn_submission_timestamp").map(_.toLong).getOrElse(0L)
    val currentTime         = System.currentTimeMillis()
    val timeElapsed         = currentTime - submissionTimestamp
    val minimumWaitTimeMs   = 4000 

    logger.info(
      s"BTNWaitingRoomController.onPageLoad: Current BTN status = $status, Session has btn_submission_initiated = $submissionInitiated, timeElapsed = ${timeElapsed}ms"
    )

    status match {
      case Some(BTNStatus.submitted) if !submissionInitiated || timeElapsed > minimumWaitTimeMs =>

        logger.info(s"BTNWaitingRoomController: Status is submitted, timeElapsed = ${timeElapsed}ms, redirecting to confirmation page")
        Future.successful(
          Redirect(routes.BTNConfirmationController.onPageLoad)
            .removingFromSession("btn_submission_initiated")
            .removingFromSession("btn_submission_timestamp")
        )

      case Some(BTNStatus.error) if !submissionInitiated || timeElapsed > minimumWaitTimeMs =>
        logger.info(s"BTNWaitingRoomController: Status is error, timeElapsed = ${timeElapsed}ms, redirecting to problem page")
        Future.successful(
          Redirect(routes.BTNProblemWithServiceController.onPageLoad)
            .removingFromSession("btn_submission_initiated")
            .removingFromSession("btn_submission_timestamp")
        )

      case Some(BTNStatus.processing) =>
        logger.info("BTNWaitingRoomController: Status is processing, showing waiting room")
        Future.successful(Ok(view()))

      case Some(BTNStatus.submitted) if submissionInitiated && timeElapsed <= minimumWaitTimeMs =>
        logger.info(s"BTNWaitingRoomController: Status is submitted, but only ${timeElapsed}ms elapsed, still showing waiting room")
        Future.successful(Ok(view()))

      case _ =>
        logger.warn("User navigated to waiting room without a valid BTN status")
        Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad))
    }
  }
}
