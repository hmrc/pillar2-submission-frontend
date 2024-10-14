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

import forms.behaviours.BooleanFieldBehaviours
import generators.Generators
import play.api.data.Form
import play.api.data.FormError
import utils.{Constants, Validation}

class AgentClientPillar2ReferenceFormProviderSpec extends BooleanFieldBehaviours with Generators {

  val requiredKey  = "agent.pillar2Ref.error.required"
  val maxLength    = Constants.EQUAL_LENGTH_15
  val regexPattern = Validation.GROUPID_REGEX

  val formProvider = new AgentClientPillar2ReferenceFormProvider
  val form: Form[String] = formProvider()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      nonEmptyRegexConformingStringWithMaxLength(regexPattern, maxLength)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, Seq())
    )
  }
}
