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

import base.SpecBase
import controllers.actions.TestAuthRetrievals.Ops
import controllers.btn.routes._
import forms.BTNLast4AccountingPeriodFormProvider
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.BTNLast4AccountingPeriodsPage
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import views.html.btn.{BTNLast4AccountingPeriodsView, BTNThresholdMetView}

import java.util.UUID
import scala.concurrent.Future

class BTNLast4AccountingPeriodsControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new BTNLast4AccountingPeriodFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val btnLast4AccountingPeriodsRoute: String = BTNLast4AccountingPeriodsController.onPageLoad(NormalMode).url
  lazy val thresholdMetRoute:              String = BTNLast4AccountingPeriodsController.onPageLoadThresholdMet.url

  private type RetrievalsType = Option[String] ~ Enrolments ~ Option[AffinityGroup] ~ Option[CredentialRole] ~ Option[Credentials]

  val enrolments: Set[Enrolment] = Set(
    Enrolment(
      key = "HMRC-PILLAR2-ORG",
      identifiers = Seq(
        EnrolmentIdentifier("PLRID", "12345678"),
        EnrolmentIdentifier("UTR", "ABC12345")
      ),
      state = "activated"
    )
  )

  val agentEnrolment: Set[Enrolment] =
    Set(
      Enrolment("HMRC-PILLAR2-ORG", List(EnrolmentIdentifier("PLRID", "XMPLR0123456789")), "Activated", Some("pillar2-auth"))
    )
  val id:           String = UUID.randomUUID().toString
  val groupId:      String = UUID.randomUUID().toString
  val providerId:   String = UUID.randomUUID().toString
  val providerType: String = UUID.randomUUID().toString

  "BTNLast4AccountingPeriodsController" when {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), enrolments)
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, btnLast4AccountingPeriodsRoute)
        when(mockSessionRepository.get(any()))
          .thenReturn(Future.successful(Some(emptyUserAnswers)))
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val result = route(application, request).value

        val view = application.injector.instanceOf[BTNLast4AccountingPeriodsView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, appConfig(application), messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET even agent want to access" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), agentEnrolment)
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()
      when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
        .thenReturn(
          Future.successful(
            Some(id) ~ pillar2AgentEnrolment ~ Some(Agent) ~ Some(User) ~ Some(Credentials(providerId, providerType))
          )
        )
      when(mockSessionRepository.get(any()))
        .thenReturn(Future.successful(Some(emptyUserAnswers)))
      when(mockSessionRepository.set(any()))
        .thenReturn(Future.successful(true))
      running(application) {
        val request = FakeRequest(GET, btnLast4AccountingPeriodsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[BTNLast4AccountingPeriodsView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, appConfig(application), messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(BTNLast4AccountingPeriodsPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), enrolments).build()

      running(application) {
        val request = FakeRequest(GET, btnLast4AccountingPeriodsRoute)

        val view = application.injector.instanceOf[BTNLast4AccountingPeriodsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode)(request, appConfig(application), messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), enrolments)
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
        val request =
          FakeRequest(POST, btnLast4AccountingPeriodsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual thresholdMetRoute
      }
    }

    "must redirect to a knockback page when a BTN is submitted" in {
      when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(submittedBTNRecord))

      val application =
        applicationBuilder()
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request = FakeRequest(GET, btnLast4AccountingPeriodsRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CheckYourAnswersController.cannotReturnKnockback.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), enrolments).build()

      running(application) {
        val request =
          FakeRequest(POST, btnLast4AccountingPeriodsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[BTNLast4AccountingPeriodsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, appConfig(application), messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET threshold met" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), enrolments)
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, thresholdMetRoute)
        when(mockSessionRepository.get(any()))
          .thenReturn(Future.successful(Some(emptyUserAnswers)))
        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val result = route(application, request).value

        val view = application.injector.instanceOf[BTNThresholdMetView]
        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, appConfig(application), messages(application)).toString
      }
    }

  }
}
