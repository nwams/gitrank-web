package models

import java.util.Date

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

case class Quickstart(
                       timestamp: Date, // This represents the week timestamp of the last contribution
                       title: String,
                       description: String,
                       url: String,
                       upvote: Int = 0,
                       downvote: Int = 0,
                       listVoters: Seq[String] = List(),
                       id: Option[Int] = None, // Present when the quickstart was retrieved from the database
                       owner: Option[Int] = None // Owner id of the quickstart
                       )

object Quickstart {
  implicit val quickstartWrites: Writes[Quickstart] = (
      (JsPath \ "timestamp").write[Date] and
      (JsPath \ "title").write[String] and
      (JsPath \ "description").write[String] and
      (JsPath \ "url").write[String] and
      (JsPath \ "upvote").write[Int] and
      (JsPath \ "downvote").write[Int] and
      (JsPath \ "listVoters").write[Seq[String]] and
      (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "owner").writeNullable[Int]
    )(unlift(Quickstart.unapply))

  implicit val quickstartReads: Reads[Quickstart] = (
      (JsPath \ "timestamp").read[Date] and
      (JsPath \ "title").read[String] and
      (JsPath \ "description").read[String] and
      (JsPath \ "url").read[String] and
      (JsPath \ "upvote").read[Int] and
      (JsPath \ "downvote").read[Int] and
      (JsPath \ "listVoters").read[Seq[String]] and
      (JsPath \ "id").readNullable[Int] and
      (JsPath \ "owner").readNullable[Int]
    )(Quickstart.apply _)
}

