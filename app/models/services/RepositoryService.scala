package models.services

import java.util.UUID
import javax.inject.Inject

import models.Repository
import models.daos.RepositoryDAO

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RepositoryService @Inject() (repoDAO: RepositoryDAO) {

  /**
   * Saves or create a repository to the database according to the current needs
   *
   * @param name name of the repo to save or update
   * @param addedLines number of added lines to the repo
   * @param removedLines number of deleted lines from the repo
   * @param score score of the repo
   * @return the saved user
   */
  def save(name: String, addedLines: Option[Int], removedLines: Option[Int], score: Option[Int]): Future[Repository] = {
    repoDAO.find(name).flatMap({
      case Some(existingRepo) =>
        repoDAO.save(existingRepo.copy(
          addedLines = addedLines.getOrElse(existingRepo.addedLines),
          removedLines = removedLines.getOrElse(existingRepo.removedLines),
          score = score.getOrElse(existingRepo.score)
        ))
      case None =>
        repoDAO.save(Repository(
          repoID = UUID.randomUUID(),
          addedLines = addedLines.getOrElse(0),
          removedLines = removedLines.getOrElse(0),
          name = name,
          score = 0
        ))
    })
  }

  /**
   * Retrieves a repository according to its name.
   *
   * @param name of the repository to be retrieved
   * @return
   */
  def retrieve(name: String): Future[Option[Repository]] = repoDAO.find(name)

  /**
   * Retrives a repository according to its UUID
   *
   * @param repoId UUID of the repository to be retrieved.
   * @return
   */
  def retrieve(repoId: UUID): Future[Option[Repository]] = repoDAO.find(repoId)
}
