package models.daos

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import models.daos.drivers.Neo4J
import play.api.libs.json.{JsArray, JsObject, JsUndefined, Json}

import models.{Contribution, User, Contributor, Repository}
import play.api.libs.ws.WSResponse

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class RepositoryDAO @Inject() (neo: Neo4J) {

  /**
   * Finds a Repository by its name.
   *
   * @param name The name of the repository to find.
   * @return The found repository or None if no repository for the given name could be found.
   */
  def find(name: String): Future[Option[Repository]] = {
    neo.cypher("MATCH (n:Repository) WHERE n.name = {name} RETURN n", Json.obj(
      "name" -> name
    )).map(neo.parseNeoRepo)
  }

  /**
   * Saves a repository.
   *
   * @param repository The repository to save.
   * @return The saved repository.
   */
  def save(repository: Repository): Future[Repository] = {
    neo.cypher("CREATE (n:Repository {props}) RETURN n", Json.obj(
      "props" -> Json.toJson(repository)
    )).map(response => repository)
  }

  /**
   * Finds a Repository by its id.
   *
   * @param repoID The ID of the repository to find.
   * @return The found repository or None if no repository for the given ID could be found.
   */
  def find(repoID: UUID): Future[Option[Repository]] = {
    neo.cypher("MATCH (n:Repository) WHERE n.repoID = {uuid} RETURN n", Json.obj(
      "uuid" -> repoID.toString
    )).map(neo.parseNeoRepo)
  }

  /**
   * Gets all the contributors for a given repository with all their contributions
   *
   * @param repoName name of the repository to look for
   * @return A Sequence of contributors
   */
  def findContributors(repoName: String): Future[Option[Seq[Contributor]]] = {
    neo.cypher("MATCH (u:User)-[c:CONTRIBUTED_TO]->(r:Repository) WHERE r.name = {name} RETURN c, u", Json.obj(
      "name" -> repoName
    )).map(response => {
      (((response.json \ "results")(0) \ "data")(0) \ "row")(0) match {
        case _ : JsUndefined => None
        case row => Some((((response.json \ "results")(0) \ "data")(0) \ "row").as[Seq[JsArray]].map(row => {
          val loginInfo = (row(1) \ "loginInfo").as[String]
          val logInfo = loginInfo.split(":")
          val user = User(
            LoginInfo(logInfo(0), logInfo(1)),
            (row(1) \ "username").asOpt[String],
            (row(1) \ "fullName").asOpt[String],
            (row(1) \ "email").asOpt[String],
            (row(1) \ "avatarUrl").asOpt[String],
            (row(1) \ "karma").as[Int]
          )

          Contributor(user, row(0).as[Contribution])
        }))
      }
    })
  }
}
