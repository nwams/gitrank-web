package models

import java.util.UUID

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }
import play.api.libs.json._
import play.api.libs.functional.syntax._

trait Identifiable {
  def loginInfo: LoginInfo
}

/**
 * The user object.
 *
 * @param loginInfo The linked login info. Uniquely identifies a user.
 * @param username the github username ex: callicles
 * @param fullName Maybe the full name of the authenticated user.
 * @param email Maybe the email of the authenticated provider.
 * @param avatarURL Maybe the avatar URL of the authenticated provider.
 * @param karma current karma of the user
 */
case class User(
                 loginInfo: LoginInfo,
                 username: Option[String],
                 fullName: Option[String],
                 email: Option[String],
                 avatarURL: Option[String],
                 karma: Int) extends Identity with Identifiable

object User {
  implicit val userWrites: Writes[User] = (
    (JsPath \ "loginInfo").write[LoginInfo] and
    (JsPath \ "username").writeNullable[String] and
    (JsPath \ "fullName").writeNullable[String] and
    (JsPath \ "email").writeNullable[String] and
    (JsPath \ "avatarURL").writeNullable[String] and
    (JsPath \ "karma").write[Int]
    )(unlift(User.unapply))
}

case class Contributor(
                        user: User,
                        contributions: Contribution)
