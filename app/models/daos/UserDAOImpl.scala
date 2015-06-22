package models.daos

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import models.User
import play.api.Play.current
import play.api.Play
import play.api.libs.json.{JsString, JsObject, JsUndefined, Json}
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
          "statement" -> """MATCH (n:User) WHERE n.loginInfo = {loginInfo} RETURN n""",
          "parameters" -> Json.obj(
            "loginInfo" -> JsString(loginInfo.providerID + ":" + loginInfo.providerKey)
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

    val jsonUser = Json.toJson(user).as[JsObject] - "loginInfo"
    val jsonToSend = jsonUser ++ Json.obj("loginInfo" -> JsString(user.loginInfo.providerID + ":" + user.loginInfo.providerKey))

    buildNeo4JRequest(request).post(Json.obj(
      "statements" -> Json.arr(
        Json.obj(
          "statement" -> """CREATE (n:User {props}) RETURN n""",
          "parameters" -> Json.obj(
            "props" -> jsonToSend
          )
        )
      )
    )).map(response => {
      response.status match {
        case 200 => {
          val json = Json.parse(response.body)
          if ((json \\ "errors").toList.isEmpty){
            throw new Exception(response.body)
          }
          user
        }
        case _ => throw new Exception("A user could not be saved - " + response.toString)
      }
    })
  }

  /**
   * Parses a WsResponse to get a unique user out of it.
   *
   * @param response response object
   * @return The parsed user.
   */
  def parseNeoUser(response: WSResponse) = {
    val neoResp = Json.parse(response.body)
    (((neoResp \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _ : JsUndefined => None
      case user => {
        val loginInfo = (user \ "loginInfo").as[String]
        Some(User(
          UUID.fromString((user \ "userID").as[String]),
          LoginInfo(loginInfo.substring(0, loginInfo.indexOf(":")), loginInfo.substring(loginInfo.indexOf(":")+1, loginInfo.length - 1)),
          (user \ "fullName").asOpt[String],
          (user \ "email").asOpt[String],
          (user \ "avatarUrl").asOpt[String]
        ))
      }
    }
  }

  /**
   * Builds a request to be sent to the neo4J database
   * @param req request to be modified
   * @return modified request
   */
  def buildNeo4JRequest(req: WSRequest) = req
      .withHeaders("Accept" -> "application/json ; charset=UTF-8", "Content-Type" -> "application/json")
      .withAuth(NEO4J_USER, NEO4J_PASSWORD, WSAuthScheme.BASIC)
      .withRequestTimeout(10000)
}
