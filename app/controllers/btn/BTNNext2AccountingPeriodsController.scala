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
import forms.BTNNext2AccountingPeriodsFormProvider
import models.Mode
import navigation.BTNNavigator
import pages.BTNNext2AccountingPeriodsPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.btn.{BTNNext2AccountingPeriodsView, BTNSubmitUKTRView}

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class BTNNext2AccountingPeriodsController @Inject() (
  override val messagesApi:               MessagesApi,
  sessionRepository:                      SessionRepository,
  navigator:                              BTNNavigator,
  @Named("EnrolmentIdentifier") identify: IdentifierAction,
  getData:                                DataRetrievalAction,
  requireData:                            DataRequiredAction,
  formProvider:                           BTNNext2AccountingPeriodsFormProvider,
  val controllerComponents:               MessagesControllerComponents,
  view:                                   BTNNext2AccountingPeriodsView,
  nilReturnView:                          BTNSubmitUKTRView
)(implicit ec:                            ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(BTNNext2AccountingPeriodsPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(BTNNext2AccountingPeriodsPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(BTNNext2AccountingPeriodsPage, mode, updatedAnswers))
      )
  }

  def onPageLoadNilReturn: Action[AnyContent] = identify(implicit request => Ok(nilReturnView()))
}
