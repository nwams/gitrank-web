package models.daos

import javax.inject.Inject
import javax.swing.tree.TreeNode

import akka.actor.Status.{Failure, Success}
import com.fasterxml.jackson.core
import com.fasterxml.jackson.core.{JsonToken, JsonParser}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.mohiva.play.silhouette.api.LoginInfo
import models.User
import models.daos.drivers.Neo4J
import play.api.libs.json.{JsObject, JsString, JsUndefined, Json}
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util

/**
 * Give access to the user object.
 */
class UserDAO @Inject() (neo: Neo4J) {

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo): Future[Option[User]] = {
    neo.cypher("MATCH (n:User) WHERE n.loginInfo = {loginInfo} RETURN n", Json.obj(
      "loginInfo" -> JsString(loginInfo.providerID + ":" + loginInfo.providerKey)
    )).map(parseNeoUser)
  }


  /**
   * Parses a WsResponse to get a unique user out of it.
   *
   * @return The seq of all users in Neo4j
   */
  def findAll(callback: (Any) => Future[Unit]): Future[Unit] = {
    val jsonParser = neo.cypherStream("MATCH (n:User) RETURN n ")
    jsonParser.onComplete{
      case util.Success(parser) =>  return parseJson(parser, callback)
      case _ => return null
    }
    return null
  }


  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userID: Int): Future[Option[User]] = {
    neo.cypher("MATCH (n:User) WHERE ID(n) = {userID} RETURN n", Json.obj(
      "userID" -> userID
    )).map(parseNeoUser)
  }

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def create(user: User): Future[User] = {

    val jsonUser = Json.toJson(user).as[JsObject] - "loginInfo"
    val jsonToSend = jsonUser ++ Json.obj("loginInfo" -> JsString(user.loginInfo.providerID + ":" + user.loginInfo.providerKey))

    neo.cypher("CREATE (n:User {props}) RETURN n", Json.obj(
      "props" -> jsonToSend
    )).map(response => user)
  }

  /**
   * Updates an existing user
   *
   * @param user The new state of the user
   * @return The saved user
   */
  def update(user: User): Future[User] = {
    val jsonUser = Json.toJson(user).as[JsObject] - "loginInfo"
    val jsonToSend = jsonUser ++ Json.obj("loginInfo" -> JsString(user.loginInfo.providerID + ":" + user.loginInfo.providerKey))

    neo.cypher(
      """
        MATCH (n:User) WHERE n.loginInfo={loginInfo}
        SET n={props}
        RETURN n
      """, Json.obj(
      "loginInfo" -> JsString(user.loginInfo.providerID + ":" + user.loginInfo.providerKey),
      "props" -> jsonToSend
    )).map(response => user)
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
          (user \ "username").as[String],
          (user \ "fullName").asOpt[String],
          (user \ "email").asOpt[String],
          (user \ "avatarURL").asOpt[String],
          (user \ "karma").as[Int],
          (user \ "publicEventsETag").asOpt[String],
          (user \ "lastPublicEventPull").asOpt[Long]
        ))
      }
    }
  }

  /**
   * Parse a stream  with a list of  objects
   * @param jsonParser json parser responsible for parsing the stream
   *
   */
  def parseJson( jsonParser: JsonParser,callback: (Any) => Future[Unit]): Future[Unit] ={
    jsonParser.setCodec(new ObjectMapper())
    jsonParser.nextFieldName() match {
      case "row" => {
        while( { val token = jsonParser.nextToken(); token != JsonToken.END_ARRAY }){
          jsonParser.nextToken() match{
            case JsonToken.START_OBJECT =>{
              val jsonTree : JsonNode = jsonParser.readValueAsTree[JsonNode]();
              val loginInfo = jsonTree.get("loginInfo").asText().split(":")
              callback(Some(User(
                LoginInfo(loginInfo(0), loginInfo(1)),
                jsonTree.get("username").asText(),
                if (jsonTree.get("fullName")!=null) Some(jsonTree.get("fullName").asText()) else Some(null),
                if (jsonTree.get("email")!=null) Some(jsonTree.get("email").asText()) else Some(null),
                if (jsonTree.get("avatarURL")!=null) Some(jsonTree.get("avatarURL").asText()) else Some(null),
                jsonTree.get("karma").asInt(),
                if (jsonTree.get("publicEventsETag")!=null) Some(jsonTree.get("publicEventsETag").asText()) else Some(null),
                if (jsonTree.get("lastPublicEventPull")!=null) Some(jsonTree.get("lastPublicEventPull").asLong()) else Some(0l)
              )))
            }
            case _ =>
          }
        }
      }
      case _ => parseJson(jsonParser, callback)
    }
    return null
  }
}
