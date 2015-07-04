package models.daos

import java.util.UUID

import com.google.inject.ImplementedBy
import models.Repository

import scala.concurrent.Future

@ImplementedBy(classOf[RepositoryDAOImpl])
trait RepositoryDAO {

  /**
   * Finds a Repository by its name.
   *
   * @param name The name of the repository to find.
   * @return The found repository or None if no repository for the given name could be found.
   */
  def find(name: String): Future[Option[Repository]]

  /**
   * Finds a Repository by its id.
   *
   * @param repoID The ID of the repository to find.
   * @return The found repository or None if no repository for the given ID could be found.
   */
  def find(repoID: UUID): Future[Option[Repository]]

  /**
   * Saves a repository.
   *
   * @param repository The repository to save.
   * @return The saved repository.
   */
  def save(repository: Repository): Future[Repository]
}
