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

import java.time.ZonedDateTime

case class Submission(submissionType: SubmissionType, receivedDate: ZonedDateTime, country: Option[String])

object Submission {
  implicit val format: OFormat[Submission] = Json.format[Submission]
}

sealed trait SubmissionType
object SubmissionType {
  case object UKTR extends SubmissionType
  case object ORN extends SubmissionType
  case object BTN extends SubmissionType
  case object GIR extends SubmissionType

  implicit val format: Format[SubmissionType] = new Format[SubmissionType] {
    override def reads(json: JsValue): JsResult[SubmissionType] =
      json.as[String] match {
        case "UKTR" => JsSuccess(UKTR)
        case "ORN"  => JsSuccess(ORN)
        case "BTN"  => JsSuccess(BTN)
        case "GIR"  => JsSuccess(GIR)
        case _      => JsError("Invalid submission type")
      }

    override def writes(obligationType: SubmissionType): JsValue =
      obligationType match {
        case UKTR => JsString("UKTR")
        case ORN  => JsString("ORN")
        case BTN  => JsString("BTN")
        case GIR  => JsString("GIR")
      }
  }
}
