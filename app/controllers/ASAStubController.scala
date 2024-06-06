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

package controllers

import config.FrontendAppConfig
import controllers.actions.{AgentIdentifierAction, DataRequiredAction, DataRetrievalAction, FeatureFlagActionFactory}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.agent._

import javax.inject.Inject
import scala.concurrent.Future

class ASAStubController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  view:                     ASAStubView,
  identify:                 AgentIdentifierAction,
  featureAction:            FeatureFlagActionFactory,
  getData:                  DataRetrievalAction,
  requireData:              DataRequiredAction
)(implicit appConfig:       FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (featureAction.asaAccessAction andThen identify.agentIdentify() andThen getData andThen requireData).async { implicit request =>
      Future.successful(Ok(view()))
    }
}
