package models

import java.util.UUID

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Writes}

/**
 * The user object.
 *
 * @param repoID The unique ID of the user.
 * @param addedLines The total number of lines added to the repository
 * @param removedLines The total number of lines removed from the repository
 * @param karmaWeight The total weight of all the karma of the people who have scored the repo.
 * @param name The name of the repository. ie gitlinks/gitrank
 * @param score The current score of the repository
 */
case class Repository(
                 repoID: UUID,
                 addedLines: Int,
                 removedLines: Int,
                 karmaWeight: Int,
                 name: String,
                 score: Int)

object Repository {
  implicit val repoWrites: Writes[Repository] = (
    (JsPath \ "repoID").write[UUID] and
      (JsPath \ "addedLines").write[Int] and
      (JsPath \ "removedLines").write[Int] and
      (JsPath \ "karmaWeight").write[Int] and
      (JsPath \ "name").write[String] and
      (JsPath \ "score").write[Int]
    )(unlift(Repository.unapply))
}

