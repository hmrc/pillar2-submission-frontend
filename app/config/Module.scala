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

package config

import com.google.inject.name.{Named, Names}
import com.google.inject.{AbstractModule, Provides}
import controllers.actions._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.{Clock, ZoneOffset}
import javax.inject.Singleton

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[DataRetrievalAction]).to(classOf[DataRetrievalActionImpl]).asEagerSingleton()
    bind(classOf[DataRequiredAction]).to(classOf[DataRequiredActionImpl]).asEagerSingleton()
    bind(classOf[DataRequiredAction]).to(classOf[DataRequiredActionImpl]).asEagerSingleton()
    bind(classOf[SubscriptionDataRetrievalAction]).to(classOf[SubscriptionDataRetrievalActionImpl]).asEagerSingleton()
    // For session based storage instead of cred based, change to SessionIdentifierAction
    bind(classOf[IdentifierAction]).to(classOf[AuthenticatedIdentifierAction]).asEagerSingleton()
    bind(classOf[IdentifierAction]).annotatedWith(Names.named("EnrolmentIdentifier")).to(classOf[EnrolmentIdentifierAction]).asEagerSingleton()

    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC))
  }

  @Provides
  @Named("pillar2Url")
  @Singleton
  def registerPillar2UrlProvider(servicesConfig: ServicesConfig): String =
    servicesConfig.baseUrl("pillar2")
}
