package models

import java.security.Timestamp

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Writes}

case class Score (
                   timestamp: Timestamp,
                   designScore: Int,
                   docScore: Int,
                   supportScore: Int,
                   maturityScore: Int,
                   karma: Int)

object Score {
  implicit val scoreWrites: Writes[Score] = (
    (JsPath \ "timestamp").write[Timestamp] and
      (JsPath \ "designScore").write[Int] and
      (JsPath \ "docScore").write[Int] and
      (JsPath \ "supportScore").write[Int] and
      (JsPath \ "maturityScore").write[Int] and
      (JsPath \ "karma").write[Int]
    )(unlift(Score.unapply))
}
