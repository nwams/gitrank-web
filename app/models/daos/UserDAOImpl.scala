package models.daos

import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import models.User
import org.anormcypher._
import play.api.Play.current
import play.api.Play
import play.api.libs.json.Json

import scala.concurrent.Future

/**
 * Give access to the user object.
 */
class UserDAOImpl extends UserDAO {

  /**
   * The list of users.
   */
  implicit val connection = Neo4jREST(
    Play.configuration.getString("neo4j.server").get,
    Play.configuration.getInt("neo4j.port").get,
    Play.configuration.getString("neo4j.endpoint").get,
    Play.configuration.getString("neo4j.username").get,
    Play.configuration.getString("neo4j.password").get
  )

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo) = {
    val userRow = Cypher(
      """
        MATCH (n:User) WHERE n.loginInfo = {loginInfo} RETURN
        n.userID, n.loginInfo, n.firstName, n.lastName, n.fullName, n.email, n.avatarUrl
      """)
      .on("loginInfo" -> Json.stringify(Json.toJson(loginInfo)))
      .apply()
      .head

    Future.successful(parseUserCypherRow(userRow))
  }

  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userID: UUID) = {
    val userRow = Cypher(
      """
        MATCH (n:User) WHERE n.userID = {userID} RETURN
        n.userID, n.loginInfo, n.firstName, n.lastName, n.fullName, n.email, n.avatarUrl
      """)
      .on("userID" -> userID.toString)
      .apply()
      .head

    Future.successful(parseUserCypherRow(userRow))
  }

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User) = {
    Cypher(
      """
        CREATE (n:User {params

        userID: {userID},
        loginInfo: {loginInfo},
        firstName: {firstName},
        lastName: {lastName},
        fullName: {fullName},
        email: {email},
        avatarUrl: {avatarUrl}

        )
      """)
      .on(
        "userID" -> user.userID.toString,
        "loginInfo" -> Json.stringify(Json.toJson(user.loginInfo)),
        "firstName" -> user.firstName,
        "lastName" -> user.lastName,
        "fullName" -> user.fullName,
        "email" -> user.email,
        "avatarUrl" -> user.avatarURL
      ).execute()
    Future.successful(user)
  }

  /**
   * Parses a User row result from the neo4j database into a User object
   *
   * @param userRow row result returned by the Cypher query
   * @return The parsed user
   */
  def parseUserCypherRow(userRow: CypherResultRow) = {
    Some(User(
      UUID.fromString(userRow[String]("userID")),
      Json.fromJson[LoginInfo](Json.parse(userRow[String]("loginInfo"))).get,
      Some(userRow[String]("firstName")),
      Some(userRow[String]("lastName")),
      Some(userRow[String]("fullName")),
      Some(userRow[String]("email")),
      Some(userRow[String]("avatarUrl"))
    ))
  }
}
