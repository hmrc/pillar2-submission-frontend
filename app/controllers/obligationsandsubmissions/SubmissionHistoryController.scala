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

package controllers.obligationsandsubmissions

import config.FrontendAppConfig
import controllers.actions._
import models.obligationsandsubmissions.{AccountingPeriodDetails, Submission}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.obligationsandsubmissions.ObligationsAndSubmissionsService
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.{HeadCell, Table, TableRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.obligationsandsubmissions.submissionhistory.{SubmissionHistoryNoSubmissionsView, SubmissionHistoryView}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class SubmissionHistoryController @Inject() (
  val controllerComponents:               MessagesControllerComponents,
  obligationsAndSubmissionsService:       ObligationsAndSubmissionsService,
  getSubscriptionData:                    SubscriptionDataRetrievalAction,
  requireSubscriptionData:                SubscriptionDataRequiredAction,
  view:                                   SubmissionHistoryView,
  viewNoSubscription:                     SubmissionHistoryNoSubmissionsView,
  @Named("EnrolmentIdentifier") identify: IdentifierAction
)(implicit ec:                            ExecutionContext, config: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getSubscriptionData andThen requireSubscriptionData).async { implicit request =>
    implicit val pillar2Id: String = request.subscriptionLocalData.plrReference
    val accountingPeriods = 7

    obligationsAndSubmissionsService
      .handleData(
        request.subscriptionLocalData.subAccountingPeriod.startDate.minusYears(accountingPeriods),
        request.subscriptionLocalData.subAccountingPeriod.endDate
      )
      .map {
        case success if success.accountingPeriodDetails.exists(_.obligations.exists(_.submissions.nonEmpty)) =>
          Ok(view(generateSubmissionHistoryTable(success.accountingPeriodDetails)))
        case _ => Ok(viewNoSubscription())
      }
      .recover { case _: Exception =>
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad(None))
      }
  }

  private def generateSubmissionHistoryTable(accountingPeriods: Seq[AccountingPeriodDetails])(implicit
    messages:                                                   Messages
  ): Seq[Table] =
    accountingPeriods.map { accountPeriod =>
      val rows = accountPeriod.obligations.flatMap(_.submissions.map(submission => createTableRows(submission)))
      createTable(accountPeriod.startDate, accountPeriod.endDate, rows)
    }

  private def createTable(startDate: LocalDate, endDate: LocalDate, rows: Seq[Seq[TableRow]])(implicit messages: Messages): Table = {
    val formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    val formattedEndDate   = endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))

    Table(
      caption = Some(s"$formattedStartDate to $formattedEndDate"),
      rows = rows,
      head = Some(
        Seq(
          HeadCell(Text(messages("submissionHistory.submissionType")), attributes = Map("scope" -> "col")),
          HeadCell(Text(messages("submissionHistory.submissionDate")), attributes = Map("scope" -> "col"))
        )
      )
    )
  }

  private def createTableRows(submission: Submission): Seq[TableRow] =
    Seq(
      TableRow(
        content = Text(submission.submissionType.toString)
      ),
      TableRow(
        content = Text(submission.receivedDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
      )
    )
}
