package models

import java.util.Date

import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, JsPath, Writes}

case class Contribution (
                          timestamp: Date,
                          addedLines: Int,
                          removedLines: Int)

object Contribution {
  implicit val contributionWrites: Writes[Contribution] = (
    (JsPath \ "timestamp").write[Date] and
      (JsPath \ "addedLines").write[Int] and
      (JsPath \ "removedLines").write[Int]
    )(unlift(Contribution.unapply))

  implicit val contributionReads: Reads[Contribution] = (
    (JsPath \ "timestamp").read[Date] and
      (JsPath \ "addedLines").read[Int] and
      (JsPath \ "removedLines").read[Int]
    )(Contribution.apply _)
}
