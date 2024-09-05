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
import controllers.actions.IdentifierAction
import models.Mode
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import views.html.BtnAccountingPeriodView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class BtnAccountingPeriodController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  view:                     BtnAccountingPeriodView,
  identify:                 IdentifierAction
)(implicit ec:              ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = identify { implicit request =>
    val list = SummaryListViewModel(
      rows = Seq(
        SummaryListRowViewModel("btn.btnAccountingPeriod.startAccountDate", value = ValueViewModel(HtmlContent(HtmlFormat.escape("7 January 2024")))),
        SummaryListRowViewModel(
          "btn.btnAccountingPeriod.endAccountDate",
          value = ValueViewModel(HtmlContent(HtmlFormat.escape("7 January 2025").toString))
        )
      )
    )
    Ok(view(list, mode))
  }

}
