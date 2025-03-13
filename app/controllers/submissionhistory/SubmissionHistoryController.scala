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

package controllers.submissionhistory

import config.FrontendAppConfig
import controllers.actions._
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.obligationsandsubmissions.ObligationsAndSubmissionsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants.SUBMISSION_ACCOUNTING_PERIODS
import views.html.submissionhistory.{SubmissionHistoryNoSubmissionsView, SubmissionHistoryView}

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class SubmissionHistoryController @Inject() (
  val controllerComponents:               MessagesControllerComponents,
  obligationsAndSubmissionsService:       ObligationsAndSubmissionsService,
  getSubscriptionData:                    SubscriptionDataRetrievalAction,
  requireSubscriptionData:                SubscriptionDataRequiredAction,
  view:                                   SubmissionHistoryView,
  viewNoSubmissions:                      SubmissionHistoryNoSubmissionsView,
  @Named("EnrolmentIdentifier") identify: IdentifierAction
)(implicit ec:                            ExecutionContext, config: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getSubscriptionData andThen requireSubscriptionData).async { implicit request =>
    val pillar2Id: String = request.subscriptionLocalData.plrReference

    obligationsAndSubmissionsService
      .handleData(
        pillar2Id,
        LocalDate.now.minusYears(SUBMISSION_ACCOUNTING_PERIODS),
        LocalDate.now
      )
      .map {
        case success if success.accountingPeriodDetails.exists(_.obligations.exists(_.submissions.nonEmpty)) =>
          Ok(view(success.accountingPeriodDetails, request.isAgent))
        case _ => Ok(viewNoSubmissions(request.isAgent))
      }
      .recover { case _: Exception =>
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad(None))
      }
  }
}
