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

package helpers

import config.FrontendAppConfig
import connectors.{ObligationConnector, SubscriptionConnector}
import controllers.actions.{AgentIdentifierAction, DataRequiredAction, DataRetrievalAction}
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import repositories.SessionRepository
import services.{ObligationService, SubscriptionService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import views.html.btn.CheckYourAnswersView

//TODO: Add all mocking instants in here.
trait AllMocks extends MockitoSugar { me: BeforeAndAfterEach =>

  val mockAuditConnector:        AuditConnector        = mock[AuditConnector]
  val mockAuthConnector:         AuthConnector         = mock[AuthConnector]
  val mockFrontendAppConfig:     FrontendAppConfig     = mock[FrontendAppConfig]
  val mockMessagesApi:           MessagesApi           = mock[MessagesApi]
  val mockSessionRepository:     SessionRepository     = mock[SessionRepository]
  val mockDataRetrievalAction:   DataRetrievalAction   = mock[DataRetrievalAction]
  val mockDataRequiredAction:    DataRequiredAction    = mock[DataRequiredAction]
  val mockCheckYourAnswersView:  CheckYourAnswersView  = mock[CheckYourAnswersView]
  val mockHttpClient:            HttpClient            = mock[HttpClient]
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
  val mockSubscriptionService:   SubscriptionService   = mock[SubscriptionService]
  val mockAgentIdentifierAction: AgentIdentifierAction = mock[AgentIdentifierAction]
  val mockObligationConnector:   ObligationConnector   = mock[ObligationConnector]
  val mockObligationService:     ObligationService     = mock[ObligationService]

  override protected def beforeEach(): Unit =
    Seq(
      mockAuditConnector,
      mockAuthConnector,
      mockFrontendAppConfig,
      mockMessagesApi,
      mockSessionRepository,
      mockDataRetrievalAction,
      mockDataRequiredAction,
      mockCheckYourAnswersView,
      mockHttpClient,
      mockSubscriptionConnector,
      mockSubscriptionService,
      mockAgentIdentifierAction
    ).foreach(Mockito.reset(_))
}
