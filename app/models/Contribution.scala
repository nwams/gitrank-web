package models

import java.security.Timestamp

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Writes}

case class Contribution (
                          timestamp: Timestamp,
                          addedLines: Int,
                          removedLines: Int)

object Contribution {
  implicit val contributionWrites: Writes[Contribution] = (
    (JsPath \ "timestamp").write[Timestamp] and
      (JsPath \ "addedLines").write[Int] and
      (JsPath \ "removedLines").write[Int]
    )(unlift(Contribution.unapply))
}
