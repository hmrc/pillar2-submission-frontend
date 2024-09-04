package forms

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class BtnRevenues750InNext2AccountingPeriodsFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "btnRevenues750InNext2AccountingPeriods.error.required"
  val invalidKey = "error.boolean"

  val form = new BtnRevenues750InNext2AccountingPeriodsFormProvider()()

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
