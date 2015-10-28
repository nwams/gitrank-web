package models

import java.util.Date

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

case class Quickstart(
                       id: Option[Int], // Present when the quickstart was retrived from the database
                       timestamp: Date, // This represents the week timestamp of the last contribution
                       title: String,
                       description: String,
                       url: String,
                       upvote: Int,
                       downvote: Int,
                       listVoters: Seq[String]
                       )

object Quickstart {
  implicit val quickstartWrites: Writes[Quickstart] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "timestamp").write[Date] and
      (JsPath \ "title").write[String] and
      (JsPath \ "description").write[String] and
      (JsPath \ "url").write[String] and
      (JsPath \ "upvote").write[Int] and
      (JsPath \ "downvote").write[Int] and
      (JsPath \ "listVoters").write[Seq[String]]
    )(unlift(Quickstart.unapply))

  implicit val quickstartReads: Reads[Quickstart] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "timestamp").read[Date] and
      (JsPath \ "title").read[String] and
      (JsPath \ "description").read[String] and
      (JsPath \ "url").read[String] and
      (JsPath \ "upvote").read[Int] and
      (JsPath \ "downvote").read[Int] and
      (JsPath \ "listVoters").read[Seq[String]]
    )(Quickstart.apply _)
}

