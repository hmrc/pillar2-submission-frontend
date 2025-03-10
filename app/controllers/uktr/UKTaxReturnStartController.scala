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

package controllers.uktr

import cats.data.OptionT.{fromOption, liftF}
import config.FrontendAppConfig
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import pages.PlrReferencePage
import pages.agent.AgentClientPillar2ReferencePage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.SubscriptionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.uktr.UKTaxReturnStartView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class UKTaxReturnStartController @Inject() (
  val controllerComponents:               MessagesControllerComponents,
  subscriptionService:                    SubscriptionService,
  sessionRepository:                      SessionRepository,
  getData:                                DataRetrievalAction,
  requireData:                            DataRequiredAction,
  view:                                   UKTaxReturnStartView,
  @Named("EnrolmentIdentifier") identify: IdentifierAction
)(implicit ec:                            ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    (for {
      optionalSessionData <- liftF(sessionRepository.get(request.userAnswers.id))
      sessionData         <- fromOption[Future](optionalSessionData)
      pillar2Id <- fromOption[Future](
                     sessionData
                       .get(PlrReferencePage)
                       .orElse(sessionData.get(AgentClientPillar2ReferencePage))
                   )
      subscriptionData <- liftF(subscriptionService.readSubscription(pillar2Id))
    } yield Ok(view(inactiveStatus = subscriptionData.accountStatus.exists(_.inactive))))
      .getOrElse(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
  }

}
