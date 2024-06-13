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

package controllers.auth

import config.FrontendAppConfig
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthController @Inject() (
  val controllerComponents:   MessagesControllerComponents,
  override val authConnector: AuthConnector,
  config:                     FrontendAppConfig,
  sessionRepository:          SessionRepository
)(implicit ec:                ExecutionContext)
    extends FrontendBaseController
    with AuthorisedFunctions
    with Logging
    with I18nSupport {

  def signOut(): Action[AnyContent] = Action.async { implicit request =>
    authorised(AuthProviders(GovernmentGateway))
      .retrieve(Retrievals.internalId) {
        case Some(internalId) =>
          sessionRepository.clear(internalId)
        case _ =>
          logger.warn(s"Unable to retrieve internal id or affinity group")
          Future.successful(Left(Redirect(controllers.routes.UnauthorisedController.onPageLoad)))
      }
      .map { _ =>
        Redirect(config.signOutUrl, Map("continue" -> Seq(config.exitSurveyUrl)))
      }
      .recover {
        case _: NoActiveSession =>
          Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
        case _: AuthorisationException =>
          Redirect(controllers.routes.UnauthorisedController.onPageLoad)
      }
  }

  def signOutNoSurvey(): Action[AnyContent] = Action.async { implicit request =>
    authorised(AuthProviders(GovernmentGateway))
      .retrieve(Retrievals.internalId) {
        case Some(internalId) =>
          sessionRepository.clear(internalId)
        case _ =>
          logger.warn(s"Unable to retrieve internal id or affinity group")
          Future.successful(Left(Redirect(controllers.routes.UnauthorisedController.onPageLoad)))
      }
      .map { _ =>
        Redirect(config.signOutUrl, Map("continue" -> Seq(routes.SignedOutController.onPageLoad.url)))
      }
      .recover {
        case _: NoActiveSession =>
          Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
        case _: AuthorisationException =>
          Redirect(controllers.routes.UnauthorisedController.onPageLoad)
      }
  }
}
