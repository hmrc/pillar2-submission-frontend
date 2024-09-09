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
import controllers.actions.{IdentifierAction, SubscriptionDataRetrievalAction}
import models.{MneOrDomestic, Mode}
import pages.{SubAccountingPeriodPage, SubMneOrDomesticPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ViewHelpers
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import views.html.BtnAccountingPeriodView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BtnAccountingPeriodController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  getData:                  SubscriptionDataRetrievalAction,
  view:                     BtnAccountingPeriodView,
  identify:                 IdentifierAction
)(implicit ec:              ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
    val dateHelper = new ViewHelpers()
    val accountsDate = request.maybeSubscriptionLocalData
      .flatMap(_.get(SubAccountingPeriodPage))
      .map { answer =>
        answer
      }

    val list = SummaryListViewModel(
      rows = Seq(
        SummaryListRowViewModel(
          "btn.btnAccountingPeriod.startAccountDate",
          value = ValueViewModel(HtmlContent(HtmlFormat.escape(accountsDate.map(d => dateHelper.formatDateGDS(d.startDate)).toString)))
        ),
        SummaryListRowViewModel(
          "btn.btnAccountingPeriod.endAccountDate",
          value = ValueViewModel(HtmlContent(HtmlFormat.escape(accountsDate.map(d => dateHelper.formatDateGDS(d.endDate.)).toString)))
        )
      )
    )
    Ok(view(list, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    request.maybeSubscriptionLocalData
      .flatMap(_.get(SubMneOrDomesticPage))
      .map { answer =>
        if (answer == MneOrDomestic.Uk)
          Future.successful(Redirect(controllers.btn.routes.BtnRevenues750In2AccountingPeriodController.onPageLoad(mode)))
        else Future.successful(Redirect(controllers.btn.routes.BtnEntitiesBothInUKAndOutsideController.onPageLoad(mode)))
      }
      .getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad(None))))

  }

}
