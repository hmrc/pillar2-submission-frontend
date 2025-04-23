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
import cats.implicits.catsStdInstancesForFuture
import config.FrontendAppConfig
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.{Mode, UserAnswers}
import pages.PlrReferencePage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.SubscriptionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.btn.BTNBeforeStartView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BTNBeforeStartController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  view:                     BTNBeforeStartView,
  identify:                 IdentifierAction,
  getData:                  DataRetrievalAction,
  requireData:              DataRequiredAction,
  subscriptionService:      SubscriptionService,
  sessionRepository:        SessionRepository
)(implicit appConfig:       FrontendAppConfig, ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    for {
      mayBeUserAnswer <- OptionT.liftF(sessionRepository.get(request.userId))
      userAnswers = mayBeUserAnswer.getOrElse(UserAnswers(request.userId))
      maybeSubscriptionLocalData <- OptionT.liftF(subscriptionService.getSubscriptionCache(request.userId))
      updatedAnswers <-
        OptionT.liftF(Future.fromTry(userAnswers.set(PlrReferencePage, maybeSubscriptionLocalData.plrReference)))
      _ <- OptionT.liftF(sessionRepository.set(updatedAnswers))
    } yield (): Unit
    (request.isAgent, appConfig.asaAccessEnabled) match {
      case (true, false) => Redirect(controllers.routes.UnauthorisedController.onPageLoad)
      case _             => Ok(view(request.isAgent, mode))
    }
  }

}
