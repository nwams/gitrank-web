package models.daos.drivers

import java.text.SimpleDateFormat
import javax.inject.Inject

import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models.daos.OAuth2InfoDAO
import models.{Quickstart, Contribution, Repository, User}
import play.api.Play
import play.api.Play.current
import play.api.libs.json.{Json, JsArray, JsValue}
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ElasticsearchAPI @Inject()(ws: WSClient) {

  val elasticSearchAPIUrl = Play.configuration.getString("elasticsearch.server").getOrElse("http://localhost:9200")
  val elasticSearchAPISearchEndpoint = Play.configuration.getString("elasticsearch.endpoint").getOrElse("/github/repository/_search")


  /**
   * Perform a search request for github repository names on elasticsearch
   * @param queryString query with repo name for full text search
   * @return ordered sequence of search results
   */
  def searchForRepos(queryString: String): Future[Seq[String]] = {
    ws.url(elasticSearchAPIUrl + elasticSearchAPISearchEndpoint).post(createRepoQueryJson(queryString))
      .map(
        response => {
          response.status match {
            case 200 => {
              ((response.json \ "hits") \ "total").as[Int] match {
                case 0 => Seq()
                case _ => ((response.json \ "hits") \ "hits").as[JsArray].value.map(jsValue => (jsValue \\ "name")(0).as[String])
              }
            }
            case _ => Seq()
          }
        }
      )
  }

  /**
   * Create json for Elasticsearch query
   * @param queryString name of the repo to be searched
   * @return json string
   */
  def createRepoQueryJson(queryString: String): String = {
    Json.toJson(
      Map(
        "query" ->
          Map(
            "match" -> Map(
              "repo.name" -> Json.toJson(queryString)
            )
          )

      )
    ).toString()
  }


}
