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

package form

import forms.mappings.Mappings
import play.api.data.Form
import utils.{Constants, Validation}

import javax.inject.Inject

class AgentClientPillar2ReferenceFormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> pillar2Id("agent.pillar2Ref.error.required")
        .verifying(
          firstError(
            equalLength(Constants.EQUAL_LENGTH_15, "agent.pillar2Ref.error.length"),
            regexp(Validation.GROUPID_REGEX, "agent.pillar2Ref.error.format")
          )
        )
    )
}
