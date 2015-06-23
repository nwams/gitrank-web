package models.daos

import javax.inject.Inject

import play.api.Play
import play.api.Play.current
import play.api.libs.json.{Json, JsObject}
import play.api.libs.ws.{WSAuthScheme, WSRequest, WSClient}

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Class to generate requests to the neo4j database and get the results.
 *
 * @param ws injected WS play service
 */
class neo4j @Inject() (ws: WSClient){

  val NEO4J_ENPOINT =
    Play.configuration.getString("neo4j.server").getOrElse("http://localhost") + ":" +
      Play.configuration.getInt("neo4j.port").getOrElse("7474") +
      Play.configuration.getString("neo4j.endpoint").getOrElse("/db/data/")

  val NEO4J_USER = Play.configuration.getString("neo4j.username").getOrElse("neo4j")
  val NEO4J_PASSWORD = Play.configuration.getString("neo4j.password").getOrElse("neo4j")

  /**
   * Sends a Cypher query to the neo4j server
   *
   * @param query Cypher query sent to the server
   * @param parameters parameter object to be used by the query. (See Cypher reference for more details)
   * @return
   */
  def cypher(query: String, parameters: JsObject) = {
    val request: WSRequest = ws.url(NEO4J_ENPOINT + "transaction/commit")

    buildNeo4JRequest(request).post(Json.obj(
      "statements" -> Json.arr(
        Json.obj(
          "statement" -> query,
          "parameters" -> parameters
        )
      )
    )).map(response => {
      response.status match {
        case 200 => {
          val json = Json.parse(response.body)
          if ((json \\ "errors").toList.isEmpty) {
            throw new Exception(response.body)
          }
          response
        }
        case _ => throw new Exception(response.body)
      }
    })
  }

  /**
   * Builds a request to be sent to the neo4J database
   * @param req request to be modified
   * @return modified request
   */
  def buildNeo4JRequest(req: WSRequest) = req
      .withAuth(NEO4J_USER, NEO4J_PASSWORD, WSAuthScheme.BASIC)
      .withHeaders("Accept" -> "application/json ; charset=UTF-8", "Content-Type" -> "application/json")
      .withRequestTimeout(10000)
}
