package models.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models.daos.drivers.{Neo4j, NeoParsers}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._

import scala.concurrent.Future

/**
 * The DAO to store the OAuth2 information.
 */
class OAuth2InfoDAO @Inject() (neo: Neo4j,
                              parser: NeoParsers) extends DelegableAuthInfoDAO[OAuth2Info] {

  /**
   * Finds the auth info which is linked with the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The retrieved auth info or None if no auth info could be retrieved for the given login info.
   */
  def find(loginInfo: LoginInfo): Future[Option[OAuth2Info]] = {
    neo.cypher("MATCH (n:User)-[:HAS_OAUTH2_INFO]->(o) WHERE n.loginInfo = {loginInfo} RETURN o", Json.obj(
      "loginInfo" -> JsString(loginInfo.providerID + ":" + loginInfo.providerKey)
    )).map(parser.parseNeoOAuth2Info)
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
        "props" -> parser.writeNeoOAuth2Info(authInfo)
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
        SET o = {props} RETURN o
      """,
      Json.obj(
        "loginInfo" -> JsString(loginInfo.providerID + ":" + loginInfo.providerKey),
        "props" -> parser.writeNeoOAuth2Info(authInfo)
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
}