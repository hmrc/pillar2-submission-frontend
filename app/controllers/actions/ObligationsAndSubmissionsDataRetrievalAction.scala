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

import models.obligationsandsubmissions.ObligationsAndSubmissionsSuccess
import models.requests.{ObligationsAndSubmissionsSuccessDataRequest, SubscriptionDataRequest}
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import repositories.SessionRepository
import services.obligationsandsubmissions.ObligationsAndSubmissionsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.JourneyCheck

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ObligationsAndSubmissionsDataRetrievalActionImpl @Inject() (
  val obligationsAndSubmissionsService: ObligationsAndSubmissionsService,
  sessionRepository:                    SessionRepository
)(implicit val executionContext:        ExecutionContext)
    extends ObligationsAndSubmissionsDataRetrievalAction
    with Logging {

  override protected def refine[A](request: SubscriptionDataRequest[A]): Future[Either[Result, ObligationsAndSubmissionsSuccessDataRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    for {
      obligationsAndSubmissionsData <-
        obligationsAndSubmissionsService.handleData(request.userId, request.subscriptionLocalData.subAccountingPeriod.startDate, LocalDate.now)
    } yield obligationsAndSubmissionsData match {
      case obligationData: ObligationsAndSubmissionsSuccess =>
        Right(
          ObligationsAndSubmissionsSuccessDataRequest(
            request.request,
            request.userId,
            request.subscriptionLocalData,
            obligationData,
            request.userAnswers,
            request.enrolments,
            request.isAgent,
            request.organisationName
          )
        )
      case _ =>
        logger.warn(s"obligations and submissions data not found")
        if (JourneyCheck.isBTNJourney(request.path)) {
          Left(Redirect(controllers.btn.routes.BTNProblemWithServiceController.onPageLoad))
        } else {
          Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }
  }
}

trait ObligationsAndSubmissionsDataRetrievalAction extends ActionRefiner[SubscriptionDataRequest, ObligationsAndSubmissionsSuccessDataRequest]
