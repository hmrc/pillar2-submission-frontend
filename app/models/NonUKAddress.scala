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

package models

/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}
import play.twirl.api.HtmlFormat

case class NonUKAddress(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: String,
  addressLine4: Option[String],
  postalCode:   Option[String],
  countryCode:  String
) {

  val field1:      String = HtmlFormat.escape(addressLine1).toString + "<br>"
  val field2:      String = if (addressLine2.isDefined) HtmlFormat.escape(addressLine2.mkString("")) + "<br>" else ""
  val field3:      String = HtmlFormat.escape(addressLine3).toString + "<br>"
  val field4:      String = if (addressLine4.isDefined) HtmlFormat.escape(addressLine4.mkString("")) + "<br>" else ""
  val postcode:    String = if (postalCode.isDefined) HtmlFormat.escape(postalCode.mkString("")) + "<br>" else ""
  val fullAddress: String = field1 + field2 + field3 + field4 + postcode

  val getAddressList: List[String] =
    List(addressLine1, addressLine2.getOrElse(""), addressLine3, addressLine4.getOrElse(""), postalCode.getOrElse(""), countryCode).filter(_.nonEmpty)

}
object NonUKAddress {
  implicit val format: OFormat[NonUKAddress] = Json.format[NonUKAddress]
}
