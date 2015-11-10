package models


import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

case class ElasticsearchResult(
                                title: String,
                                owner: String
                                )

object ElasticsearchResult {
  implicit val elasticsearchResultWrites: Writes[ElasticsearchResult] = (
    (JsPath \ "title").write[String] and
      (JsPath \ "owner").write[String]
    )(unlift(ElasticsearchResult.unapply))
}

