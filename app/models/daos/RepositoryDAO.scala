package models.daos

import javax.inject.Inject

import models.Repository
import models.daos.drivers.{Neo4j, NeoParsers}
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class RepositoryDAO @Inject() (neo: Neo4j,
                              parser: NeoParsers) {

  /**
   * Finds a Repository by its name.
   *
   * @param name The name of the repository to find.
   * @return The found repository or None if no repository for the given name could be found.
   */
  def find(name: String): Future[Option[Repository]] = {
    neo.cypher("MATCH (n:Repository) WHERE n.name = {name} RETURN n", Json.obj(
      "name" -> name
    )).map(parser.parseNeoRepo)
  }

  /**
   * Finds a Repository by its id.
   *
   * @param repoID The ID of the repository to find.
   * @return The found repository or None if no repository for the given ID could be found.
   */
  def find(repoID: Int): Future[Option[Repository]] = {
    neo.cypher("MATCH (n:Repository) WHERE n.repoID = {id} RETURN n", Json.obj(
      "id" -> repoID
    )).map(parser.parseNeoRepo)
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
    neo.cypher("MATCH (n:Repository) WHERE n.name = {name} SET n={props} RETURN n", Json.obj(
      "name" -> repository.name,
      "props" -> Json.toJson(repository)
    )).map(response => repository)
  }
}
