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
import models.obligationsandsubmissions.ObligationStatus
import models.{MneOrDomestic, Mode}
import pages.SubMneOrDomesticPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.HtmlFormat
import services.obligationsandsubmissions.ObligationsAndSubmissionsService
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ViewHelpers
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import views.html.btn.{BTNAccountingPeriodView, BTNReturnSubmittedView}

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class BTNAccountingPeriodController @Inject() (
  val controllerComponents:               MessagesControllerComponents,
  getSubscriptionData:                    SubscriptionDataRetrievalAction,
  requireSubscriptionData:                SubscriptionDataRequiredAction,
  btnStatus:                              BTNStatusAction,
  obligationsAndSubmissionsService:       ObligationsAndSubmissionsService,
  dateHelper:                             ViewHelpers,
  view:                                   BTNAccountingPeriodView,
  viewReturnSubmitted:                    BTNReturnSubmittedView,
  @Named("EnrolmentIdentifier") identify: IdentifierAction
)(implicit ec:                            ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getSubscriptionData andThen requireSubscriptionData andThen btnStatus.subscriptionRequest).async { implicit request =>
      val pillar2Id                 = request.subscriptionLocalData.plrReference
      val changeAccountingPeriodUrl = appConfig.changeAccountingPeriodUrl
      val subAccountingPeriod       = request.subscriptionLocalData.subAccountingPeriod
      val accountStatus             = request.subscriptionLocalData.accountStatus.forall(_.inactive)

      val accountingPeriods = {
        val startDate = HtmlFormat.escape(dateHelper.formatDateGDS(subAccountingPeriod.startDate))
        val endDate   = HtmlFormat.escape(dateHelper.formatDateGDS(subAccountingPeriod.endDate))

        SummaryListViewModel(
          rows = Seq(
            SummaryListRowViewModel(
              key = "btn.returnSubmitted.startAccountDate",
              value = ValueViewModel(HtmlContent(startDate))
            ),
            SummaryListRowViewModel(
              key = "btn.returnSubmitted.endAccountDate",
              value = ValueViewModel(HtmlContent(endDate))
            )
          )
        )
      }

      obligationsAndSubmissionsService
        .handleData(pillar2Id, subAccountingPeriod.startDate, subAccountingPeriod.endDate)
        .map {
          case success if !accountStatus && success.accountingPeriodDetails.exists(_.obligations.exists(_.status == ObligationStatus.Fulfilled)) =>
            Ok(viewReturnSubmitted(accountingPeriods))
          case success if !accountStatus && success.accountingPeriodDetails.exists(_.obligations.exists(_.status == ObligationStatus.Open)) =>
            Ok(view(accountingPeriods, mode, changeAccountingPeriodUrl))
          case _ =>
            Redirect(controllers.btn.routes.BTNProblemWithServiceController.onPageLoad)
        }
        .recover { case _ =>
          Redirect(controllers.btn.routes.BTNProblemWithServiceController.onPageLoad)
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
      .getOrElse(Future.successful(Redirect(controllers.btn.routes.BTNProblemWithServiceController.onPageLoad)))
  }
}
