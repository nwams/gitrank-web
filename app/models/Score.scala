package models

import java.util.Date

import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, JsPath, Writes}

import scala.collection.immutable.HashMap

case class Score(
                  timestamp: Date,
                  designScore: Int,
                  docScore: Int,
                  supportScore: Int,
                  maturityScore: Int,
                  feedback: String,
                  karma: Int) {
  def toMap(): HashMap[String, String] = {
    HashMap("scoreDocumentation" -> Integer.toString(docScore),
      "scoreMaturity" -> Integer.toString(maturityScore),
      "scoreDesign" -> Integer.toString(designScore),
      "scoreSupport" -> Integer.toString(supportScore),
      "feedback" -> feedback)
  }
}

object Score {
  implicit val scoreWrites: Writes[Score] = (
    (JsPath \ "timestamp").write[Date] and
      (JsPath \ "designScore").write[Int] and
      (JsPath \ "docScore").write[Int] and
      (JsPath \ "supportScore").write[Int] and
      (JsPath \ "maturityScore").write[Int] and
      (JsPath \ "feedback").write[String] and
      (JsPath \ "karma").write[Int]
    )(unlift(Score.unapply))

  implicit val scoreReads: Reads[Score] = (
    (JsPath \ "timestamp").read[Date] and
      (JsPath \ "designScore").read[Int] and
      (JsPath \ "docScore").read[Int] and
      (JsPath \ "supportScore").read[Int] and
      (JsPath \ "maturityScore").read[Int] and
      (JsPath \ "feedback").read[String] and
      (JsPath \ "karma").read[Int]
    )(Score.apply _)
}

case class Feedback(
                     user: User,
                     score: Score
                     )