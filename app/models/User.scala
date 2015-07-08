package models

import java.util.UUID

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * The user object.
 *
 * @param userID The unique ID of the user.
 * @param loginInfo The linked login info.
 * @param username the github username ex: callicles
 * @param fullName Maybe the full name of the authenticated user.
 * @param email Maybe the email of the authenticated provider.
 * @param avatarURL Maybe the avatar URL of the authenticated provider.
 * @param karma current karma of the user
 */
case class User(
                 userID: UUID,
                 loginInfo: LoginInfo,
                 username: String,
                 fullName: Option[String],
                 email: Option[String],
                 avatarURL: Option[String],
                 karma: Int) extends Identity

object User {
  implicit val userWrites: Writes[User] = (
    (JsPath \ "userID").write[UUID] and
    (JsPath \ "loginInfo").write[LoginInfo] and
    (JsPath \ "username").write[String] and
    (JsPath \ "fullName").writeNullable[String] and
    (JsPath \ "email").writeNullable[String] and
    (JsPath \ "avatarURL").writeNullable[String] and
    (JsPath \ "karma").write[Int]
    )(unlift(User.unapply))
}
