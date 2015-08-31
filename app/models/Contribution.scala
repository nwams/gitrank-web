package models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

case class Contribution (
                          timestamp: Long, // This represents the week timestamp of the last contribution
                          addedLines: Int,
                          removedLines: Int,
                          currentWeekBuffer: Option[String]
                        )

object Contribution {
  implicit val contributionWrites: Writes[Contribution] = (
    (JsPath \ "timestamp").write[Long] and
    (JsPath \ "addedLines").write[Int] and
    (JsPath \ "removedLines").write[Int] and
    (JsPath \ "currentWeekBuffer").writeNullable[String]
    )(unlift(Contribution.unapply))

  implicit val contributionReads: Reads[Contribution] = (
    (JsPath \ "timestamp").read[Long] and
    (JsPath \ "addedLines").read[Int] and
    (JsPath \ "removedLines").read[Int] and
    (JsPath \ "currentWeekBuffer").readNullable[String]
    )(Contribution.apply _)
}
