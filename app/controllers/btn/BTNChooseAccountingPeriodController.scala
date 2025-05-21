/*
 * Copyright 2025 HM Revenue & Customs
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
import controllers.filteredAccountingPeriodDetails
import forms.BTNChooseAccountingPeriodFormProvider
import models.Mode
import navigation.BTNNavigator
import pages.BTNChooseAccountingPeriodPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.obligationsandsubmissions.ObligationsAndSubmissionsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.btn.BTNChooseAccountingPeriodView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class BTNChooseAccountingPeriodController @Inject() (
  override val messagesApi:               MessagesApi,
  sessionRepository:                      SessionRepository,
  navigator:                              BTNNavigator,
  @Named("EnrolmentIdentifier") identify: IdentifierAction,
  getSubscriptionData:                    SubscriptionDataRetrievalAction,
  requireSubscriptionData:                SubscriptionDataRequiredAction,
  requireObligationData:                  ObligationsAndSubmissionsDataRetrievalAction,
  btnStatus:                              BTNStatusAction,
  formProvider:                           BTNChooseAccountingPeriodFormProvider,
  obligationsAndSubmissionsService:       ObligationsAndSubmissionsService,
  val controllerComponents:               MessagesControllerComponents,
  view:                                   BTNChooseAccountingPeriodView
)(implicit ec:                            ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getSubscriptionData andThen requireSubscriptionData andThen btnStatus.subscriptionRequest andThen requireObligationData) {
      implicit request =>
//      obligationsAndSubmissionsService
//        .handleData(
//          request.subscriptionLocalData.plrReference,
//          request.subscriptionLocalData.subAccountingPeriod.startDate,
//          request.subscriptionLocalData.subAccountingPeriod.endDate
//        )
//        .map { success =>
        val accountingPeriods = filteredAccountingPeriodDetails(request.obligationsAndSubmissionsSuccessData.accountingPeriodDetails).zipWithIndex
        val form              = formProvider()
        val preparedForm = request.userAnswers
          .get(BTNChooseAccountingPeriodPage)
          .flatMap { chosenPeriod =>
            accountingPeriods.find(_._1 == chosenPeriod).map { case (_, index) =>
              form.fill(index)
            }
          }
          .getOrElse(form)
        Ok(view(preparedForm, mode, request.isAgent, request.organisationName, accountingPeriods))
//        }
//        .recover { case _ =>
//          Redirect(controllers.btn.routes.BTNProblemWithServiceController.onPageLoad)
//        }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getSubscriptionData andThen requireSubscriptionData andThen btnStatus.subscriptionRequest andThen requireObligationData).async {
      implicit request =>
//      obligationsAndSubmissionsService
//        .handleData(
//          request.subscriptionLocalData.plrReference,
//          request.subscriptionLocalData.subAccountingPeriod.startDate,
//          request.subscriptionLocalData.subAccountingPeriod.endDate
//        )
//        .flatMap { success =>
        val accountingPeriods = filteredAccountingPeriodDetails(request.obligationsAndSubmissionsSuccessData.accountingPeriodDetails).zipWithIndex
        val form              = formProvider()
        form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  view(
                    formWithErrors,
                    mode,
                    request.isAgent,
                    request.organisationName,
                    accountingPeriods
                  )
                )
              ),
            value =>
              accountingPeriods.find { case (_, index) => index == value } match {
                case Some((chosenPeriod, _)) =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(BTNChooseAccountingPeriodPage, chosenPeriod))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(controllers.btn.routes.BTNAccountingPeriodController.onPageLoad(mode))
                case None =>
                  Future.successful(Redirect(controllers.btn.routes.BTNProblemWithServiceController.onPageLoad))
              }
          )
//        }
//        .recover { case _ =>
//          Redirect(controllers.btn.routes.BTNProblemWithServiceController.onPageLoad)
//        }
    }
}
