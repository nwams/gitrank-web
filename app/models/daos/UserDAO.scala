package models.daos

import javax.inject.Inject

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.mohiva.play.silhouette.api.LoginInfo
import models.daos.drivers.Neo4J
import models.{Repository, User}
import play.api.libs.json._
import play.api.libs.ws._

import scala.collection.mutable.ArrayBuffer
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
     Future( neo.cypherStream("MATCH (n:User) RETURN n ").onComplete{
        case util.Success(parser) =>  parseJson(parser, callback)
        case _ =>
      })
  }

  /**
   * Returns all user that contributed to a specific repo
   * @param repository Repository that has received contributions
   */
  def findAllFromRepo(repository: Repository): Future[Seq[Option[User]]] = {
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
      case user => {
        neo.parseSingleUser(user)
      }
    }
  }

  /**
   * Parses a WsResponse to get all users out of it.
   *
   * @param response response object
   * @return The parsed users.
   */
  def parseNeoUsers(response: WSResponse): Seq[Option[User]] = (Json.parse(response.body) \\ "user").map(neo.parseSingleUser(_))


  /**
   * Parse a stream  with a list of  objects
   * @param jsonParser json parser responsible for parsing the stream
   * @param callback callback function for each item
   */
  def parseJson( jsonParser: JsonParser,callback: (Any) => Future[Unit]): Unit ={
    jsonParser.setCodec(new ObjectMapper())
    jsonParser.nextFieldName() match {
      case "row" => {
        Stream.cons( parseJsonFragment(jsonParser,callback), Stream.continually(parseJsonFragment(jsonParser,callback))).find( x => jsonParser.nextToken() == JsonToken.END_ARRAY);
      }
      case _ => parseJson(jsonParser, callback)
    }}

  /**
   *
   * Parse a fragment of a User Json
   * @param jsonParser parser with the whole json stream
   * @param callback callback function for each item
   */
  def parseJsonFragment(jsonParser: JsonParser,callback: (Any) => Future[Unit])= {
      jsonParser.getCurrentToken() match{
        case JsonToken.START_OBJECT =>{
          val jsonTree : JsonNode = jsonParser.readValueAsTree[JsonNode]();
          val loginInfo = jsonTree.get("loginInfo").asText().split(":")
          callback(Some(User(
            LoginInfo(loginInfo(0), loginInfo(1)),
            jsonTree.get("username").asText(),
            Option(jsonTree.get("fullName")).map(_.asText),
            Option(jsonTree.get("email")).map(_.asText),
            Option(jsonTree.get("avatarURL")).map(_.asText),
            jsonTree.get("karma").asInt(),
            Option(jsonTree.get("publicEventsETag")).map(_.asText),
            Option(jsonTree.get("lastPublicEventPull")).map(_.asLong())
          )))
        }
        case _ =>
      }
  }
 }
