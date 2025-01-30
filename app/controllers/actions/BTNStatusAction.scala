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

import controllers.btn.routes._
import models.btn.BTNStatus
import models.requests.{DataRequest, SubscriptionDataRequest}
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BTNStatusAction @Inject() (val sessionRepository: SessionRepository)(implicit val ec: ExecutionContext) extends Logging {

  def subscriptionRequest: ActionRefiner[SubscriptionDataRequest, SubscriptionDataRequest] =
    new ActionRefiner[SubscriptionDataRequest, SubscriptionDataRequest] {
      override protected def refine[A](request: SubscriptionDataRequest[A]): Future[Either[Result, SubscriptionDataRequest[A]]] =
        sessionRepository.get(request.userId).flatMap { maybeUserAnswers =>
          maybeUserAnswers.flatMap(_.get(BTNStatus)) match {
            case Some(BTNStatus.submitted) => Future.successful(Left(Redirect(CheckYourAnswersController.cannotReturnKnockback)))
            case _                         => Future.successful(Right(request))
          }
        }

      override protected def executionContext: ExecutionContext = ec
    }

  def dataRequest: ActionRefiner[DataRequest, DataRequest] =
    new ActionRefiner[DataRequest, DataRequest] {
      override protected def refine[A](request: DataRequest[A]): Future[Either[Result, DataRequest[A]]] = {
        println(sessionRepository.getClass.getCanonicalName)
        sessionRepository.get(request.userId).flatMap { maybeUserAnswers =>
          println("I'm here slkdfj;lskdjf;lskdjf;lsdkjf;lkj")
          println(s"$maybeUserAnswers")
          maybeUserAnswers.flatMap(_.get(BTNStatus)) match {
            case Some(BTNStatus.submitted) => Future.successful(Left(Redirect(CheckYourAnswersController.cannotReturnKnockback)))
            case _                         => Future.successful(Right(request))
          }
        }
      }

      override protected def executionContext: ExecutionContext = ec
    }
}
