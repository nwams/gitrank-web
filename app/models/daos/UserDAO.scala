package models.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import models.daos.drivers.{Neo4j, NeoParsers}
import models.{Repository, User}
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util

/**
 * Give access to the user object.
 */
class UserDAO @Inject() (neo: Neo4j,
                        parser: NeoParsers) {

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo): Future[Option[User]] = {
    neo.cypher("MATCH (n:User) WHERE n.loginInfo = {loginInfo} RETURN n", Json.obj(
      "loginInfo" -> JsString(loginInfo.providerID + ":" + loginInfo.providerKey)
    )).map(parser.parseNeoUser)
  }


  /**
   * Parses a WsResponse to get a unique user out of it.
   * @param callback callback function for each user
   *
   */
  def findAll(callback: (Any) => Future[Unit]): Future[Unit] = {
     Future( neo.cypherStream("MATCH (n:User) RETURN n ").onComplete{
        case util.Success(toParse) => parser.parseJson(toParse, callback)
        case _ =>
      })
  }

  /**
   * Returns all user that contributed to a specific repo
   * @param repository Repository that has received contributions
   */
  def findAllFromRepo(repository: Repository): Future[Seq[User]] = {
      neo.cypher("MATCH (u:User)-[c:CONTRIBUTED_TO]->(r:Repository) "+
        "WHERE  r.name={repoName} RETURN u",
        Json.obj("repoName" -> repository.name)
      ).map(parser.parseNeoUsers)
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
    )).map(parser.parseNeoUser)
  }

  /**
   * Finds a user by its username.
   *
   * @param username The username of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(username: String): Future[Option[User]] = {
    neo.cypher("MATCH (n:User) WHERE n.username = {username} RETURN n", Json.obj(
      "username" -> username
    )).map(parser.parseNeoUser)
  }

  /**
    * Find all the repositories scored by a user
    *
    * @param user that scored the repositories
    * @return a list of repositories scored by the user
    */
  def findScoredRepositories(user: User): Future[Seq[Repository]] = {
    neo.cypher(
      """
        MATCH (n:User)-[:SCORED]->(r:Repository)
        WHERE n.loginInfo = {loginInfo}
        RETURN r
      """, Json.obj(
        "loginInfo" -> JsString(user.loginInfo.providerID + ":" + user.loginInfo.providerKey)
      )).map(parser.parseNeoRepos)
  }

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def create(user: User): Future[User] = {

    val jsonUser = Json.toJson(user).as[JsObject] - "loginInfo"
    val jsonToSend = jsonUser ++ Json.obj(
      "loginInfo" -> JsString(user.loginInfo.providerID + ":" + user.loginInfo.providerKey)
    )

    neo.cypher("CREATE (n:User {props}) RETURN n", Json.obj(
      "props" -> jsonToSend
    )).map(response =>user)
  }

  /**
   * Updates an existing user
   *
   * @param user The new state of the user
   * @return The saved user
   */
  def update(user: User): Future[User] = {
    val jsonUser = Json.toJson(user).as[JsObject] - "loginInfo"
    val jsonToSend = jsonUser ++ Json.obj(
      "loginInfo" -> JsString(user.loginInfo.providerID + ":" + user.loginInfo.providerKey)
    )

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
 }
