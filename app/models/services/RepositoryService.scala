package models.services

import javax.inject.Inject

import models.{Contribution, Repository}
import models.daos.{ContributionDAO, RepositoryDAO}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RepositoryService @Inject() (repoDAO: RepositoryDAO, contributionDAO: ContributionDAO) {

  /**
   * Saves or create a repository to the database according to the current needs
   *
   * @param name name of the repo to save or update
   * @param addedLines number of added lines to the repo
   * @param removedLines number of deleted lines from the repo
   * @param score score of the repo
   * @return the saved Repository
   */
  def save(id: Int, name: String, addedLines: Option[Int], removedLines: Option[Int], karmaWeight: Option[Int], score: Option[Int]): Future[Repository] = {
    repoDAO.find(id).flatMap({
      case Some(existingRepo) =>
        repoDAO.update(existingRepo.copy(
          name = name,
          addedLines = addedLines.getOrElse(existingRepo.addedLines),
          removedLines = removedLines.getOrElse(existingRepo.removedLines),
          karmaWeight = karmaWeight.getOrElse(existingRepo.karmaWeight),
          score = score.getOrElse(existingRepo.score)
        ))
      case None =>
        repoDAO.create(Repository(
          repoID = id,
          addedLines = addedLines.getOrElse(0),
          removedLines = removedLines.getOrElse(0),
          karmaWeight = karmaWeight.getOrElse(0),
          name = name,
          score = score.getOrElse(0)
        ))
    })
  }

  /**
   * Adds a contribution to the given repository
   *
   * @param userName name of the user who has contributed to the repository
   * @param repoName name of the repository he has contributed to
   * @param contribution Contribution itself.
   * @return
   */
  def addContribution(userName: String, repoName: String, contribution: Contribution) = contributionDAO.add(userName,repoName, contribution)

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
  def retrieve(repoId: Int): Future[Option[Repository]] = repoDAO.find(repoId)
}
