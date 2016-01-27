package models.daos.drivers

import javax.inject.Inject

import dispatch._
import play.api.Configuration
import play.api.libs.json.{JsArray, Json}
import utils.ElasticQueryParser

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ElasticsearchAPI @Inject()(configuration: Configuration) {

  val elasticSearchAPIUrl = configuration.getString("elasticsearch.server").getOrElse("http://localhost:9200")
  val elasticSearchAPISearchEndpoint = configuration.getString("elasticsearch.endpoint").getOrElse("/github/repository/_search")


  /**
   * Perform a search request for github repository names on elasticsearch
   *
   * @param queryString query with repo name for full text search
   * @return ordered sequence of search results
   */
  def searchForRepos(queryString: String): Future[Seq[String]] = {
    val post = url(elasticSearchAPIUrl + elasticSearchAPISearchEndpoint) << createRepoQueryJson(queryString)
    Http(post OK as.String).map(body => parseBody(body))
  }

  /**
    * Parses the body of an elasticsearch response, as json
    *
    * @param body String body containing the json with the response
    * @return Sequence of string results
    */
  def parseBody(body: String): Seq[String] = {
    val json = Json.parse(body)
    ((json \ "hits") \ "total").as[Int] match {
      case 0 => Seq()
      case _ => ((json \ "hits") \ "hits").as[JsArray].value.map(jsValue => (jsValue \\ "name")(0).as[String])
    }
  }

  /**
   * Create json for Elasticsearch query
   *
   * @param queryString name of the repo to be searched
   * @return json string
   */
  def createRepoQueryJson(queryString: String): String = {
    val newString: String = ElasticQueryParser.escapeCharsForQuery(queryString)
    val queryField = Map(
      "default_field" -> "repo.name",
      "query" -> (newString + "*")
    )
    Json.toJson(
      Map(
        "query" ->
          Map(
            "query_string" -> queryField
          )
      )
    ).toString()
  }
}
