package models.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models.daos.drivers.Neo4J

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

/**
 * The DAO to store the OAuth2 information.
 */
class OAuth2InfoDAO @Inject() (neo: Neo4J) extends DelegableAuthInfoDAO[OAuth2Info] {

  implicit val OAuth2InfoWrites: Writes[OAuth2Info] = (
    (JsPath \ "accessToken").write[String] and
      (JsPath \ "tokenType").writeNullable[String] and
      (JsPath \ "expiresIn").writeNullable[Int] and
      (JsPath \ "refreshToken").writeNullable[String] and
      (JsPath \ "params").writeNullable[Map[String, String]]
    )(unlift(OAuth2Info.unapply))

  /**
   * Finds the auth info which is linked with the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The retrieved auth info or None if no auth info could be retrieved for the given login info.
   */
  def find(loginInfo: LoginInfo): Future[Option[OAuth2Info]] = {
    neo.cypher("MATCH (n:User)-[:HAS_OAUTH2_INFO]->(o) WHERE n.loginInfo = {loginInfo} RETURN o", Json.obj(
      "loginInfo" -> JsString(loginInfo.providerID + ":" + loginInfo.providerKey)
    )).map(parseNeoOAuth2Info)
  }

  /**
   * Adds new auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be added.
   * @param authInfo The auth info to add.
   * @return The added auth info.
   */
  def add(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = {
    neo.cypher(
      """MATCH (n:User) WHERE n.loginInfo = {loginInfo}
         CREATE (n)-[:HAS_OAUTH2_INFO]->(o:Oauth2Info {props})
         RETURN o
      """,
      Json.obj(
        "loginInfo" -> JsString(loginInfo.providerID + ":" + loginInfo.providerKey),
        "props" -> writeNeoOAuth2Info(authInfo)
      )
    ).map(res => authInfo)
  }

  /**
   * Updates the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be updated.
   * @param authInfo The auth info to update.
   * @return The updated auth info.
   */
  def update(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = {
    neo.cypher(
      """
        MATCH (n:User)-[:HAS_OAUTH2_INFO]->(o) WHERE n.loginInfo = {loginInfo}
        SET n = {props} RETURN o
      """,
      Json.obj(
        "loginInfo" -> JsString(loginInfo.providerID + ":" + loginInfo.providerKey),
        "props" -> writeNeoOAuth2Info(authInfo)
      )
    ).map(res => authInfo)
  }

  /**
   * Saves the auth info for the given login info.
   *
   * This method either adds the auth info if it doesn't exists or it updates the auth info
   * if it already exists.
   *
   * @param loginInfo The login info for which the auth info should be saved.
   * @param authInfo The auth info to save.
   * @return The saved auth info.
   */
  def save(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = {
    find(loginInfo).flatMap {
      case Some(_) => update(loginInfo, authInfo)
      case None => add(loginInfo, authInfo)
    }
  }

  /**
   * Removes the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be removed.
   * @return A future to wait for the process to be completed.
   */
  def remove(loginInfo: LoginInfo): Future[Unit] = {
    neo.cypher(
      """
        MATCH (n:User)-[r:HAS_OAUTH2_INFO]->(o) WHERE n.loginInfo = {loginInfo}
        DELETE r,o
      """,
      Json.obj(
        "loginInfo" -> JsString(loginInfo.providerID + ":" + loginInfo.providerKey)
      )
    ).map(res => Unit)
  }

  /**
   * Parses a WsResponse to get a unique OAuth2Info out of it.
   *
   * @param response response object
   * @return The parsed OAuth2Info.
   */
  def parseNeoOAuth2Info(response: WSResponse) = (((Json.parse(response.body) \ "results")(0) \ "data")(0) \ "row")(0).asOpt[OAuth2Info]

  def writeNeoOAuth2Info(authInfo: OAuth2Info) = {
    val jsonAuth = Json.toJson(authInfo)
    val params = (jsonAuth \ "params").asOpt[JsValue]

    params match {
      case None => jsonAuth
      case Some(p) => (jsonAuth.as[JsObject] - "params") ++ Json.obj("params" -> Json.stringify(p))
    }
  }
}