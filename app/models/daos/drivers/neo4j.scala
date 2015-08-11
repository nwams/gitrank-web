package models.daos.drivers

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import models.{Contribution, Score, User, Repository}
import play.api.Play
import play.api.Play.current
import play.api.libs.json.{JsUndefined, JsObject, Json}
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
   * Parse a Repository from a neo4j row result
   *
   * @param response response object
   * @return The parsed Repository.
   */
  def parseNeoRepo(response: WSResponse): Option[Repository] = {
    (((Json.parse(response.body) \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _: JsUndefined => None
      case repo => {
        Some(Repository(
          UUID.fromString((repo \ "repoID").as[String]),
          (repo \ "addedLines").as[Int],
          (repo \ "removedLines").as[Int],
          (repo \ "karmaWeight").as[Int],
          (repo \ "name").as[String],
          (repo \ "score").as[Int]
        ))
      }
    }
  }

  /**
   * Parses a WsResponse to get a unique user out of it.
   *
   * @param response response object
   * @return The parsed user.
   */
  def parseNeoUser(response: WSResponse): Option[User] = {
    (((Json.parse(response.body) \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _: JsUndefined => None
      case user => {
        val loginInfo = (user \ "loginInfo").as[String]
        val logInfo = loginInfo.split(":")
        Some(User(
          LoginInfo(logInfo(0), logInfo(1)),
          (user \ "username").asOpt[String],
          (user \ "fullName").asOpt[String],
          (user \ "email").asOpt[String],
          (user \ "avatarUrl").asOpt[String],
          (user \ "karma").as[Int]
        ))
      }
    }
  }

  /**
   * Parses a neo Score into a model
   *
   * @param response response from neo
   * @return
   */
  def parseNeoScore(response: WSResponse): Option[Score] = {
    (((Json.parse(response.body) \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _: JsUndefined => None
      case score => score.asOpt[Score]
    }
  }

  /**
   * Parses a neo4j response to get a Contribution out of it.
   *
   * @param response neo4j response
   * @return parsed contribution or None
   */
  def parseNeoContribution(response: WSResponse): Option[Contribution] = {
    (((Json.parse(response.body) \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _: JsUndefined => None
      case repo => repo.asOpt[Contribution]
    }
  }
}
