package models.daos.drivers

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import javax.inject.Inject

import com.fasterxml.jackson.core.{JsonFactory, JsonParser}
import com.mohiva.play.silhouette.api.LoginInfo
import models.User
import play.api.Play
import play.api.Play.current
import play.api.libs.iteratee.Iteratee
import play.api.libs.json._
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Class to generate requests to the neo4j database and get the results.
 *
 * @param ws injected WS play service
 */
class Neo4J @Inject() (ws: WSClient){

  val NEO4J_ENDPOINT =
    Play.configuration.getString("neo4j.server").getOrElse("http://localhost") + ":" +
      Play.configuration.getInt("neo4j.port").getOrElse("7474") +
      Play.configuration.getString("neo4j.endpoint").getOrElse("/db/data/")

  val NEO4J_USER = Play.configuration.getString("neo4j.username").getOrElse("neo4j")
  val NEO4J_PASSWORD = Play.configuration.getString("neo4j.password").getOrElse("neo4j")

  ws.url(NEO4J_ENDPOINT)
    .withAuth(NEO4J_USER, NEO4J_PASSWORD, WSAuthScheme.BASIC)
    .withHeaders("Accept" -> "application/json ; charset=UTF-8", "Content-Type" -> "application/json")
    .withRequestTimeout(10000)
    .get()
    .map(res => {
    res.status match {
      case 200 =>
        val json = Json.parse(res.body)
        if ((json \ "errors").as[Seq[JsObject]].nonEmpty) {
          throw new Exception(res.body)
        }
      case _ => throw new Exception("Could not Connect to the Neo4j Database")
    }
  })

  /**
   * Sends a Cypher query to the neo4j server
   *
   * @param query Cypher query sent to the server
   * @param parameters parameter object to be used by the query. (See Cypher reference for more details)
   * @return
   */
  def cypher(query: String, parameters: JsObject): Future[WSResponse] = {
    val request: WSRequest = ws.url(NEO4J_ENDPOINT + "transaction/commit")

    buildNeo4JRequest(request).post(Json.obj(
      "statements" -> Json.arr(
        Json.obj(
          "statement" -> query,
          "parameters" -> parameters
        )
      )
    )).map(response => {
      response.status match {
        case 200 =>
          val json = Json.parse(response.body)
          if ((json \ "errors").as[Seq[JsObject]].nonEmpty) {
            throw new Exception(response.body)
          }
          response
        case _ => throw new Exception(response.body)
      }
    })
  }

  /**
   * Builds a request to be sent to the neo4J database
   *
   * @param req request to be modified
   * @return modified request
   */
  def buildNeo4JRequest(req: WSRequest): WSRequest = req
      .withAuth(NEO4J_USER, NEO4J_PASSWORD, WSAuthScheme.BASIC)
      .withHeaders("Accept" -> "application/json ; charset=UTF-8", "Content-Type" -> "application/json")
      .withRequestTimeout(10000)

  /**
   * Execute a query with a stream result
   * @param query query for using on the neo4j database
   *
   */
  def cypherStream(query: String): Future[JsonParser] = {
    val outputStream = new ByteArrayOutputStream()
    buildNeo4JRequest(ws.url(NEO4J_ENDPOINT + "transaction/commit"))
      .withHeaders("X-Stream" -> "true")
      .withMethod("POST")
      .withBody(Json.obj(
      "statements" -> Json.arr(
        Json.obj(
          "statement" -> query
        )
      )
    ))
      .stream().map{
      case (response,body) =>
        if(response.status == 200){
          val iteratee = Iteratee.foreach[Array[Byte]] { bytes =>
            outputStream.write(bytes)
          }
          (body |>>> iteratee).andThen {
            case result =>
              outputStream.close()
              result.get
          }
          new JsonFactory().createParser(new ByteArrayInputStream(outputStream.toByteArray));
        }else{
          throw new Exception("Failure in getting users");
        }

    }
  }

  /**
   * Parser responsible for parsing the jsLookup
   *
   * @param user jsLookup for single user
   * @return a single user
   */
  def parseSingleUser(user : JsLookup): User = {
    val loginInfo = (user \ "loginInfo").as[String]
    val logInfo = loginInfo.split(":")
    User(
      LoginInfo(logInfo(0), logInfo(1)),
      (user \ "username").as[String],
      (user \ "fullName").asOpt[String],
      (user \ "email").asOpt[String],
      (user \ "avatarURL").asOpt[String],
      (user \ "karma").as[Int],
      (user \ "publicEventsETag").asOpt[String],
      (user \ "lastPublicEventPull").asOpt[Long]
    )
  }
}
