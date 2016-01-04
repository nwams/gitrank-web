package models.daos.drivers

import java.io.InputStreamReader
import javax.inject.Inject

import com.fasterxml.jackson.core.{JsonFactory, JsonParser}
import dispatch.Defaults._
import dispatch._
import play.api.Configuration
import play.api.libs.json._

import scala.concurrent.Future

/**
 * Class to generate requests to the neo4j database and get the results.
 */
class Neo4j @Inject()(configuration: Configuration) {

  val neo4jEndpoint =
    configuration.getString("neo4j.server").getOrElse(Neo4jDefaults.serverAddress) +
      configuration.getString("neo4j.endpoint").getOrElse(Neo4jDefaults.endpoint)

  val neo4jUser = configuration.getString("neo4j.username").getOrElse(Neo4jDefaults.user)
  val neo4jPassword = configuration.getString("neo4j.password").getOrElse(Neo4jDefaults.password)

  /**
   * Sends a Cypher query to the neo4j server
   *
   * @param query Cypher query sent to the server
   * @param parameters parameter object to be used by the query. (See Cypher reference for more details)
   * @return
   */
  def cypher(query: String, parameters: JsObject): Future[JsValue] = {
    val request: Req = url(neo4jEndpoint + "transaction/commit")

    val jsonBody = Json.obj(
      "statements" -> Json.arr(
        Json.obj(
          "statement" -> query,
          "parameters" -> parameters
        )
      )
    )

    val post = buildNeo4JRequest(request) << jsonBody.toString()

    Http(post OK as.String).map(body => {
      val json = Json.parse(body)
      if ((json \ "errors").as[Seq[JsObject]].nonEmpty) {
        throw new Exception(body)
      }
      json
    })
  }

  /**
   * Builds a request to be sent to the neo4J database
   *
   * @param req request to be modified
   * @return modified request
   */
  def buildNeo4JRequest(req: Req): Req = req
      .as(neo4jUser, neo4jPassword)
      .addHeader("Accept", "application/json ; charset=UTF-8")
      .addHeader("Content-Type", "application/json")

  /**
   * Execute a query with a stream result
    *
   * @param query query for using on the neo4j database
   * @return JsonParser containing the stream of json
   */
  def cypherStream(query: String): JsonParser = {

    val post = buildNeo4JRequest(url(neo4jEndpoint + "transaction/commit"))
      .addHeader("X-Stream", "true") << Json.obj(
        "statements" -> Json.arr(
          Json.obj(
            "statement" -> query
          )
        )
      ).toString()

    val stream = Http(post OK as.Response(_.getResponseBodyAsStream)).apply
    new JsonFactory().createParser(new InputStreamReader(stream))
  }
}
