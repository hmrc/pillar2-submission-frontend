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

package models.obligation

import play.api.libs.json._

sealed trait ObligationType
object ObligationType {
  case object UKTR extends ObligationType
  case object GIR extends ObligationType

  val values: Seq[ObligationType] = Seq(
    UKTR,
    GIR
  )

  implicit val format: Format[ObligationType] = new Format[ObligationType] {
    override def reads(json: JsValue): JsResult[ObligationType] =
      json.as[String] match {
        case "UKTR" => JsSuccess(UKTR)
        case "GIR"  => JsSuccess(GIR)
        case _      => JsError("Invalid obligation type")
      }

    override def writes(obligationType: ObligationType): JsValue =
      obligationType match {
        case UKTR => JsString("UKTR")
        case GIR  => JsString("GIR")
      }
  }

}
