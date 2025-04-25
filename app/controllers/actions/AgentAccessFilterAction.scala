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

package controllers.actions

import config.FrontendAppConfig
import models.requests.IdentifierRequest
import play.api.mvc.Results.Redirect
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentAccessFilterAction @Inject() (appConfig: FrontendAppConfig)(implicit ec: ExecutionContext) extends ActionFilter[IdentifierRequest] {

  override def filter[A](request: IdentifierRequest[A]): Future[Option[Result]] =
    Future.successful {
      (request.isAgent, appConfig.asaAccessEnabled) match {
        case (true, false) => Some(Redirect(controllers.routes.UnauthorisedController.onPageLoad))
        case _             => None
      }
    }

  override protected def executionContext: ExecutionContext = ec
}
