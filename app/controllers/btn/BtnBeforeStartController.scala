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

import cats.data.OptionT
import cats.data.OptionT.liftF
import cats.implicits.catsStdInstancesForFuture
import config.FrontendAppConfig
import connectors.SubscriptionConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.{Mode, UserAnswers}
import pages.PlrReferencePage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.SubscriptionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.btn.BtnBeforeStartView

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BtnBeforeStartController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  view:                     BtnBeforeStartView,
  identify:                 IdentifierAction,
  getData:                  DataRetrievalAction,
  requireData:              DataRequiredAction,
  subscriptionService:      SubscriptionService,
  sessionRepository:        SessionRepository
)(implicit appConfig:       FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    for {
      mayBeUserAnswer <- OptionT.liftF(sessionRepository.get(request.userId))
      userAnswers = mayBeUserAnswer.getOrElse(UserAnswers(request.userId))
      maybeSubscriptionLocalData <- OptionT.liftF(subscriptionService.getSubscriptionCache(request.userId))
      updatedAnswers <-
        OptionT.liftF(Future.fromTry(userAnswers.set(PlrReferencePage, maybeSubscriptionLocalData.plrReference)))
      _ <- OptionT.liftF(sessionRepository.set(updatedAnswers))
    } yield (): Unit
    Ok(view(mode))
  }

}
