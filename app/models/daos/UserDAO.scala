package models.daos

import javax.inject.Inject
import javax.swing.tree.TreeNode

import akka.actor.Status.{Failure, Success}
import com.fasterxml.jackson.core
import com.fasterxml.jackson.core.{JsonFactory, JsonToken, JsonParser}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.mohiva.play.silhouette.api.LoginInfo
import models.{Repository, User}
import models.daos.drivers.Neo4J
import play.api.libs.json._
import play.api.libs.ws._

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
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
   * @param callback callback function for each user
   *
   */
  def findAll(callback: (Any) => Future[Unit]): Future[Unit] = {
    Future{
      val jsonParser = neo.cypherStream("MATCH (n:User) RETURN n ")
      jsonParser.onComplete{
        case util.Success(parser) =>  parseJson(parser, callback)
        case _ =>
      }
    }
  }

  /**
   * Returns all user that contributed to a specific repo
   * @param repository Repository that has received contributions
   */
  def findAllFromRepo(repository: Repository): Future[Seq[User]] = {
      neo.cypher("MATCH (u:User)-[c:CONTRIBUTED_TO]->(r:Repository) "+
        "WHERE  r.name={repoName} RETURN u", Json.obj("repoID" -> repository.repoID)).map(parseNeoUsers)
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
        parseSingleUser(user)
      }
    }
  }

  /**
   * Parses a WsResponse to get all users out of it.
   *
   * @param response response object
   * @return The parsed users.
   */
  def parseNeoUsers(response: WSResponse): Seq[User] = {
    var listUser = ArrayBuffer[User]()
    ((((Json.parse(response.body) \ "results")(0) \ "data")(0) \ "row")(0) \\ "user").map(_.as[JsObject]).toList.foreach{
        listUser +=  parseSingleUser(_).get
    }
    listUser
  }

  /**
   * Parser responsible for parsing the jsLookup
   * @param user jsLookup for single user
   * @return a single user
   */
  private def  parseSingleUser(user : JsLookup): Option[User] = {
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
  /**
   * Parse a stream  with a list of  objects
   * @param jsonParser json parser responsible for parsing the stream
   *
   */
  def parseJson( jsonParser: JsonParser,callback: (Any) => Future[Unit]): Unit ={
    jsonParser.setCodec(new ObjectMapper())
    jsonParser.nextFieldName() match {
      case "row" => {
        while( { val token = jsonParser.nextToken(); token != JsonToken.END_ARRAY }){
          jsonParser.getCurrentToken() match{
            case JsonToken.START_OBJECT =>{
              val jsonTree : JsonNode = jsonParser.readValueAsTree[JsonNode]();
              val loginInfo = jsonTree.get("loginInfo").asText().split(":")
              callback(Some(User(
                LoginInfo(loginInfo(0), loginInfo(1)),
                jsonTree.get("username").asText(),
                Some(jsonTree.get("fullName")).map(test =>{test.asText()}),
                Some(jsonTree.get("email")).map(test =>{test.asText()}),
                Some(jsonTree.get("avatarURL")).map(test =>{test.asText()}),
                jsonTree.get("karma").asInt(),
                Some(jsonTree.get("publicEventsETag")).map(test =>{test.asText()}),
                Some(jsonTree.get("lastPublicEventPull")).map(test =>{test.asLong()})
              )))
            }
            case _ =>
          }
        }
      }
      case _ => parseJson(jsonParser, callback)
    }}
}
