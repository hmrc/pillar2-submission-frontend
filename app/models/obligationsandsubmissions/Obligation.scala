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

package models.obligationsandsubmissions

import play.api.libs.json._

case class Obligation(obligationType: ObligationType, status: ObligationStatus, canAmend: Boolean, submissions: Seq[Submission])

object Obligation {
  implicit val format: OFormat[Obligation] = Json.format[Obligation]
}

sealed trait ObligationStatus
object ObligationStatus {
  case object Open extends ObligationStatus
  case object Fulfilled extends ObligationStatus

  implicit val format: Format[ObligationStatus] = new Format[ObligationStatus] {
    override def reads(json: JsValue): JsResult[ObligationStatus] =
      json.as[String] match {
        case "Open"      => JsSuccess(Open)
        case "Fulfilled" => JsSuccess(Fulfilled)
        case _           => JsError("Invalid obligation status")
      }

    override def writes(ObligationStatus: ObligationStatus): JsValue =
      ObligationStatus match {
        case Open      => JsString("Open")
        case Fulfilled => JsString("Fulfilled")
      }
  }
}

sealed trait ObligationType
object ObligationType {
  case object Pillar2TaxReturn extends ObligationType
  case object GlobeInformationReturn extends ObligationType

  implicit val format: Format[ObligationType] = new Format[ObligationType] {
    override def reads(json: JsValue): JsResult[ObligationType] =
      json.as[String] match {
        case "Pillar2TaxReturn"       => JsSuccess(Pillar2TaxReturn)
        case "GlobeInformationReturn" => JsSuccess(GlobeInformationReturn)
        case _                        => JsError("Invalid obligation type")
      }

    override def writes(obligationType: ObligationType): JsValue =
      obligationType match {
        case Pillar2TaxReturn       => JsString("Pillar2TaxReturn")
        case GlobeInformationReturn => JsString("GlobeInformationReturn")
      }
  }
}
