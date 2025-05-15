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
import controllers.actions._
import models.obligationsandsubmissions.SubmissionType.BTN
import models.obligationsandsubmissions.{AccountingPeriodDetails, ObligationStatus}
import models.{MneOrDomestic, Mode}
import pages.{BTNChooseAccountingPeriodPage, SubMneOrDomesticPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.HtmlFormat
import services.obligationsandsubmissions.ObligationsAndSubmissionsService
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ViewHelpers
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import views.html.btn.{BTNAccountingPeriodView, BTNAlreadyInPlaceView, BTNReturnSubmittedView}

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class BTNAccountingPeriodController @Inject() (
  val controllerComponents:               MessagesControllerComponents,
  getSubscriptionData:                    SubscriptionDataRetrievalAction,
  requireSubscriptionData:                SubscriptionDataRequiredAction,
  btnStatus:                              BTNStatusAction,
  obligationsAndSubmissionsService:       ObligationsAndSubmissionsService,
  dateHelper:                             ViewHelpers,
  accountingPeriodView:                   BTNAccountingPeriodView,
  viewReturnSubmitted:                    BTNReturnSubmittedView,
  btnAlreadyInPlaceView:                  BTNAlreadyInPlaceView,
  @Named("EnrolmentIdentifier") identify: IdentifierAction
)(implicit ec:                            ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getSubscriptionData andThen requireSubscriptionData andThen btnStatus.subscriptionRequest).async { implicit request =>
      val pillar2Id                 = request.subscriptionLocalData.plrReference
      val changeAccountingPeriodUrl = appConfig.changeAccountingPeriodUrl
      val accountStatus             = request.subscriptionLocalData.accountStatus.forall(_.inactive)

      def accountingPeriods(startDate: LocalDate, endDate: LocalDate) = {
        val start = HtmlFormat.escape(dateHelper.formatDateGDS(startDate))
        val end   = HtmlFormat.escape(dateHelper.formatDateGDS(endDate))

        SummaryListViewModel(
          rows = Seq(
            SummaryListRowViewModel(
              key = "btn.returnSubmitted.startAccountDate",
              value = ValueViewModel(HtmlContent(start))
            ),
            SummaryListRowViewModel(
              key = "btn.returnSubmitted.endAccountDate",
              value = ValueViewModel(HtmlContent(end))
            )
          )
        )
      }

      val accountingPeriod: Future[AccountingPeriodDetails] = request.userAnswers.get(BTNChooseAccountingPeriodPage) match {
        case Some(details) =>
          Future.successful(details)
        case None =>
          obligationsAndSubmissionsService
            .handleData(pillar2Id, request.subscriptionLocalData.subAccountingPeriod.startDate, LocalDate.now())
            .map(_.accountingPeriodDetails.filterNot(_.startDate.isAfter(LocalDate.now())).filterNot(_.dueDate.isBefore(LocalDate.now())))
            .map {
              case singleAccountingPeriod :: Nil =>
                singleAccountingPeriod
              case e =>
                throw new RuntimeException(s"Expected one single accounting period but received: $e")
            }
      }

      accountingPeriod
        .map {
          case period if !accountStatus && period.obligations.exists(_.submissions.exists(_.submissionType == BTN)) =>
            Ok(btnAlreadyInPlaceView())
          case period if !accountStatus && period.obligations.exists(_.status == ObligationStatus.Fulfilled) =>
            Ok(
              viewReturnSubmitted(
                accountingPeriods(period.startDate, period.endDate),
                false,
                period
              )
            )
          case period if !accountStatus && period.obligations.exists(_.status == ObligationStatus.Open) =>
            Ok(
              accountingPeriodView(
                accountingPeriods(period.startDate, period.endDate),
                mode,
                changeAccountingPeriodUrl,
                request.isAgent,
                request.organisationName,
                request.userAnswers.get(BTNChooseAccountingPeriodPage).isDefined
              )
            )
          case _ =>
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad(None))
        }
        .recover { case _ =>
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad(None))
        }
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getSubscriptionData).async { implicit request =>
    request.maybeSubscriptionLocalData
      .flatMap(_.get(SubMneOrDomesticPage))
      .map { answer =>
        if (answer == MneOrDomestic.UkAndOther) {
          Future.successful(Redirect(controllers.btn.routes.BTNEntitiesInsideOutsideUKController.onPageLoad(mode)))
        } else {
          Future.successful(Redirect(controllers.btn.routes.BTNEntitiesInUKOnlyController.onPageLoad(mode)))
        }
      }
      .getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad(None))))
  }
}
