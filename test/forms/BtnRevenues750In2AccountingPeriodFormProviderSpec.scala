package forms

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class BtnRevenues750In2AccountingPeriodFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "btnRevenues750In2AccountingPeriod.error.required"
  val invalidKey = "error.boolean"

  val form = new BtnRevenues750In2AccountingPeriodFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
