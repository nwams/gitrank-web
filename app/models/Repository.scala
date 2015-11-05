package models

import java.util.Date

import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, JsPath, Writes}

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
                 repoID: Int,
                 addedLines: Int,
                 removedLines: Int,
                 karmaWeight: Int,
                 name: String,
                 score: Float)

object Repository {
  implicit val repoWrites: Writes[Repository] = (
    (JsPath \ "repoID").write[Int] and
      (JsPath \ "addedLines").write[Int] and
      (JsPath \ "removedLines").write[Int] and
      (JsPath \ "karmaWeight").write[Int] and
      (JsPath \ "name").write[String] and
      (JsPath \ "score").write[Float]
    )(unlift(Repository.unapply))
  implicit val repoReads: Reads[Repository] = (
      (JsPath \ "repoID").read[Int] and
      (JsPath \ "addedLines").read[Int] and
      (JsPath \ "removedLines").read[Int] and
      (JsPath \ "karmaWeight").read[Int] and
      (JsPath \ "name").read[String] and
      (JsPath \ "score").read[Float]
    )(Repository.apply _)
}

case class GitHubRepo(
                       id: Int,
                       name: String,
                       updatedAt: Date,
                       stars: Int,
                       description: String
                     )

object GitHubRepo {
  implicit val repoWrites: Writes[GitHubRepo] = (
    (JsPath \ "id").write[Int] and
      (JsPath \ "full_name").write[String] and
      (JsPath \ "updated_at").write[Date] and
      (JsPath \ "stargazers_count").write[Int] and
      (JsPath \ "description").write[String]
    )(unlift(GitHubRepo.unapply))

  implicit val repoReads: Reads[GitHubRepo] = (
    (JsPath \ "id").read[Int] and
      (JsPath \ "full_name").read[String] and
      (JsPath \ "updated_at").read[Date] and
      (JsPath \ "stargazers_count").read[Int] and
      (JsPath \ "description").read[String]
    )(GitHubRepo.apply _)
}

