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

package base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import config.FrontendAppConfig
import controllers.actions._
import helpers.{AllMocks, SubscriptionLocalDataFixture, ViewInstances}
import models.UserAnswers
import models.requests.{DataRequest, IdentifierRequest, OptionalDataRequest}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues, TryValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.{HeaderNames, HttpProtocol, MimeTypes, Status}
import play.api.i18n.{DefaultLangs, Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.{EssentialActionCaller, FakeRequest, ResultExtractors, Writeables}
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils

import scala.concurrent.{ExecutionContext, Future}

trait SpecBase
    extends AnyWordSpec
    with OptionValues
    with ScalaFutures
    with BeforeAndAfterEach
    with Matchers
    with TryValues
    with Results
    with HttpProtocol
    with AllMocks
    with ResultExtractors
    with Status
    with MimeTypes
    with Writeables
    with EssentialActionCaller
    with HeaderNames
    with ViewInstances
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with SubscriptionLocalDataFixture
    with WireMockServerHandler {

  val userAnswersId: String = "id"
  val PlrReference:  String = "XMPLR0123456789"

  type AgentRetrievalsType = Option[String] ~ Enrolments ~ Option[AffinityGroup] ~ Option[CredentialRole]

  val pillar2AgentEnrolment: Enrolments =
    Enrolments(Set(Enrolment("HMRC-AS-AGENT", List(EnrolmentIdentifier("AgentReference", "1234")), "Activated", None)))

  val pillar2AgentEnrolmentWithDelegatedAuth: Enrolments = Enrolments(
    Set(
      Enrolment("HMRC-AS-AGENT", List(EnrolmentIdentifier("AgentReference", "1234")), "Activated", None),
      Enrolment("HMRC-PILLAR2-ORG", List(EnrolmentIdentifier("PLRID", PlrReference)), "Activated", Some("pillar2-auth"))
    )
  )

  val pillar2OrganisationEnrolment: Enrolments = Enrolments(
    Set(Enrolment("HMRC-PILLAR2-ORG", List(EnrolmentIdentifier("PLRID", PlrReference)), "Activated", None))
  )

  def emptyUserAnswers:        UserAnswers       = UserAnswers(userAnswersId)
  implicit lazy val ec:        ExecutionContext  = scala.concurrent.ExecutionContext.Implicits.global
  implicit lazy val hc:        HeaderCarrier     = HeaderCarrier()
  implicit lazy val appConfig: FrontendAppConfig = new FrontendAppConfig(configuration, servicesConfig)

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val languageUtil = new LanguageUtils(new DefaultLangs(), configuration)
  def appConfig(app: Application): FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  def preAuthenticatedActionBuilders: AuthenticatedIdentifierAction =
    new AuthenticatedIdentifierAction(
      mockAuthConnector,
      mockFrontendAppConfig,
      new BodyParsers.Default
    ) {
      override def refine[A](request: Request[A]): Future[Either[Result, IdentifierRequest[A]]] =
        Future.successful(Right(IdentifierRequest(request, "internalId")))
    }

  def preAuthenticatedEnrolmentActionBuilders(enrolments: Option[Set[Enrolment]] = None): AuthenticatedIdentifierAction =
    new AuthenticatedIdentifierAction(
      mockAuthConnector,
      mockFrontendAppConfig,
      new BodyParsers.Default
    ) {
      override def refine[A](request: Request[A]): Future[Either[Result, IdentifierRequest[A]]] = {
        val identifierRequest = IdentifierRequest(request, "internalId", enrolments.getOrElse(Set.empty))
        Future.successful(Right(identifierRequest))
      }
    }

  def preDataRequiredActionImpl: DataRequiredActionImpl = new DataRequiredActionImpl()(ec) {
    override protected def refine[A](request: OptionalDataRequest[A]): Future[Either[Result, DataRequest[A]]] =
      Future.successful(Right(DataRequest(request.request, request.userId, emptyUserAnswers)))
  }

  def preDataRetrievalActionImpl: DataRetrievalActionImpl = new DataRetrievalActionImpl(mockSessionRepository)(ec) {
    override protected def transform[A](request: IdentifierRequest[A]): Future[OptionalDataRequest[A]] =
      Future(OptionalDataRequest(request.request, request.userId, Some(emptyUserAnswers)))(ec)
  }

  protected def applicationBuilder(
    userAnswers:    Option[UserAnswers] = None,
    enrolments:     Set[Enrolment] = Set.empty,
    additionalData: Map[String, Any] = Map.empty
  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        Configuration.from(
          Map(
            "metrics.enabled"         -> "false",
            "auditing.enabled"        -> false,
            "features.grsStubEnabled" -> true
          ) ++ additionalData
        )
      )
      .overrides(
        bind[Enrolments].toInstance(Enrolments(enrolments)),
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[IdentifierAction].qualifiedWith("EnrolmentIdentifier").to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )

  protected def stubResponse(expectedEndpoint: String, expectedStatus: Int, expectedBody: String): StubMapping =
    server.stubFor(
      post(urlEqualTo(s"$expectedEndpoint"))
        .willReturn(
          aResponse()
            .withStatus(expectedStatus)
            .withBody(expectedBody)
        )
    )

  protected def stubGet(expectedEndpoint: String, expectedStatus: Int, expectedBody: String): StubMapping =
    server.stubFor(
      get(urlEqualTo(s"$expectedEndpoint"))
        .willReturn(
          aResponse()
            .withStatus(expectedStatus)
            .withBody(expectedBody)
        )
    )

  protected def stubDelete(expectedEndpoint: String, expectedStatus: Int, expectedBody: String): StubMapping =
    server.stubFor(
      delete(urlEqualTo(s"$expectedEndpoint"))
        .willReturn(
          aResponse()
            .withStatus(expectedStatus)
            .withBody(expectedBody)
        )
    )

  protected def stubResponseForPutRequest(expectedEndpoint: String, expectedStatus: Int, responseBody: Option[String] = None): StubMapping =
    server.stubFor(
      put(urlEqualTo(expectedEndpoint))
        .willReturn(
          aResponse()
            .withStatus(expectedStatus)
            .withBody(responseBody.getOrElse(""))
        )
    )

}
