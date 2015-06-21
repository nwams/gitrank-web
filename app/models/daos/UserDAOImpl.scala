package models.daos

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import models.User
import play.api.Play.current
import play.api.Play
import play.api.libs.json.{JsUndefined, Json}
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Give access to the user object.
 */
class UserDAOImpl @Inject() (ws: WSClient) extends UserDAO {


  val NEO4J_ENPOINT =
    Play.configuration.getString("neo4j.server").get + ":" +
    Play.configuration.getInt("neo4j.port").get +
    Play.configuration.getString("neo4j.endpoint").get

  val NEO4J_USER = Play.configuration.getString("neo4j.username").get

  val NEO4J_PASSWORD = Play.configuration.getString("neo4j.password").get

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo) = {
    val request: WSRequest = ws.url(NEO4J_ENPOINT + "transaction/commit")

    buildNeo4JRequest(request).post(Json.obj(
      "statements" -> Json.arr(
        Json.obj(
          "statement" -> """MATCH (n:User) WHERE n.LoginInfo = {LoginInfo} RETURN n""",
          "parameters" -> Json.obj(
            "LoginInfo" -> Json.stringify(Json.toJson(loginInfo))
          )
        )
      )
    )).map(res => parseNeoUser(res))
  }

  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userID: UUID) = {
    val request: WSRequest = ws.url(NEO4J_ENPOINT + "transaction/commit")

    buildNeo4JRequest(request).post(Json.obj(
      "statements" -> Json.arr(
        Json.obj(
          "statement" -> """MATCH (n:User) WHERE n.userID = {userID} RETURN n""",
          "parameters" -> Json.obj(
            "userID" -> userID.toString
          )
        )
      )
    )).map(res => parseNeoUser(res))
  }

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User) = {

    val request: WSRequest = ws.url(NEO4J_ENPOINT + "transaction/commit")

    buildNeo4JRequest(request).post(Json.obj(
      "statements" -> Json.arr(
        Json.obj(
          "statement" -> """CREATE (n:User {props}) RETURN n""",
          "parameters" -> Json.obj(
            "props" ->Json.obj(
              "userID" -> user.userID.toString,
              "loginInfo" -> Json.stringify(Json.toJson(user.loginInfo)),
              "fullName" -> user.fullName,
              "email" -> user.email,
              "avatarUrl" -> user.avatarURL
            )
          )
        )
      )
    )).map(response => {
      response.status match {
        case 200 => user
        case _ => throw new Exception("A user could not be saved - " + response.toString)
      }
    })
  }

  def parseNeoUser(response: WSResponse) = {
    val neoResp = Json.parse(response.body)
    (((neoResp \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _ : JsUndefined => None
      case user => Some(User(
        UUID.fromString((user \ "userID").as[String]),
        LoginInfo((user \ "loginInfo" \ "providerID").as[String], (user \ "loginInfo" \ "providerKey").as[String]),
        (user \ "fullName").asOpt[String],
        (user \ "email").asOpt[String],
        (user \ "avatarUrl").asOpt[String]
      ))
    }
  }

  def buildNeo4JRequest(req: WSRequest) = req
      .withHeaders("Accept" -> "application/json ; charset=UTF-8", "Content-Type" -> "application/json")
      .withAuth(NEO4J_USER, NEO4J_PASSWORD, WSAuthScheme.BASIC)
      .withRequestTimeout(10000)
}
