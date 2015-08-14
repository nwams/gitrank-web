package models.daos

import java.util.UUID
import javax.inject.Inject

import models.daos.drivers.Neo4J
import play.api.libs.json.{JsUndefined, Json}

import models.Repository
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
    )).map(parseNeoRepo)
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
    )).map(parseNeoRepo)
  }

  /**
   * Saves a repository.
   *
   * @param repository The repository to save.
   * @return The saved repository.
   */
  def create(repository: Repository): Future[Repository] = {
    neo.cypher("CREATE (n:Repository {props}) RETURN n", Json.obj(
      "props" -> Json.toJson(repository)
    )).map(response => repository)
  }

  /**
   * Updates an existing repository with the new repository values
   *
   * @param repository The updated repository
   * @return The saved repository
   */
  def update(repository: Repository): Future[Repository] = {
    neo.cypher("MATCH (n:Repository) WHERE n.repoID = {uuid} SET n={props} RETURN n", Json.obj(
      "uuid" -> repository.repoID.toString,
      "props" -> Json.toJson(repository)
    )).map(response => repository)
  }

  /**
   * Parse a Repository from a neo4j row result
   *
   * @param response response object
   * @return The parsed Repository.
   */
  def parseNeoRepo(response: WSResponse): Option[Repository] = {
    (((Json.parse(response.body) \ "results")(0) \ "data")(0) \ "row")(0) match {
      case _: JsUndefined => None
      case repo => {
        Some(Repository(
          UUID.fromString((repo \ "repoID").as[String]),
          (repo \ "addedLines").as[Int],
          (repo \ "removedLines").as[Int],
          (repo \ "karmaWeight").as[Int],
          (repo \ "name").as[String],
          (repo \ "score").as[Int]
        ))
      }
    }
  }
}
