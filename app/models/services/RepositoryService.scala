package models.services

import javax.inject.Inject

import models.daos.drivers.GitHubAPI
import models.daos.{ScoreDAO, ContributionDAO, RepositoryDAO, UserDAO}
import models.{Score, Contribution, Repository, User}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RepositoryService @Inject() (
                                    repoDAO: RepositoryDAO,
                                    contributionDAO: ContributionDAO,
                                    userDAO: UserDAO,
                                    scoreDAO: ScoreDAO,
                                    gitHub: GitHubAPI,
                                    userService: UserService) {

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
  def saveContribution(userName: String, repoName: String, contribution: Contribution) = {
    contributionDAO.find(userName, repoName).flatMap({
      case Some(existingContribution) => {
        contributionDAO.update(userName , repoName, existingContribution.copy(
          timestamp = contribution.timestamp,
          addedLines = existingContribution.addedLines + contribution.addedLines - parseWeekAddedLines(existingContribution.currentWeekBuffer),
          removedLines = existingContribution.removedLines + contribution.removedLines - parseWeekDeletedLines(existingContribution.currentWeekBuffer),
          currentWeekBuffer = contribution.currentWeekBuffer
        ))
      }
      case None => contributionDAO.add(userName,repoName, contribution)
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
   * Retrieves a repository according to its UUID
   *
   * @param repoId UUID of the repository to be retrieved.
   * @return
   */
  def retrieve(repoId: Int): Future[Option[Repository]] = repoDAO.find(repoId)

  /**
   * Parses a string representing the buffer of the current week contribution getting the added lines
   *
   * @param currentWeekBuffer String containing the added lines this week already counted for needed to be extracted.
   * @return count of the added lines already accounted for extracted as an Int
   */
  private def parseWeekAddedLines(currentWeekBuffer: Option[String]): Int = {
    val str = currentWeekBuffer.getOrElse("a0d0")
    str.substring(0, str.indexOf("d")).toInt
  }

  /**
   * Parses a string representing the buffer of the current week contribution getting the deleted lines
   *
   * @param currentWeekBuffer String containing the deleted lines this week already counted for needed to be extracted.
   * @return count of the deleted lines already accounted for extracted as an Int
   */
  private def parseWeekDeletedLines(currentWeekBuffer: Option[String]): Int = {
    val str = currentWeekBuffer.getOrElse("a0d0")
    str.substring(str.indexOf("d"), str.length).toInt
  }

  /**
   * Gets all the contributors for a given repository with all their contributions
   *
   * @param repoName name of the repository to look for, "owner/repo"
   * @return A Sequence of contributors
   */
  def findContributors(repoName: String): Future[Seq[User]] ={
    repoDAO.find(repoName).map({
      case repo => return userDAO.findAllFromRepo(repo.get)
    })
  }

  /**
   * Function that check if a repository exists in the database, if it does, returns the corresponding repository
   * If not, it checks if the repository exists on GitHub. if it does, it returns the corresponding repository
   * If not, it returns None
   *
   * @param identity identity of the current user, can be None if no user is connected
   * @param repoName "owner/repo"
   *
   * @return Future of Option of repository
   */
  def getFromNeoOrGitHub (identity: Option[User], repoName: String): Future[Option[Repository]] = {
    retrieve(repoName).flatMap((repoOption: Option[Repository]) => repoOption match {
      case Some(repository) => Future(Some(repository))
      case None => identity match {
        case None => gitHub.getRepository(repoName)
        case Some(user) => userService.getOAuthInfo(user).flatMap(oAuthInfo => gitHub.getRepository(repoName, oAuthInfo))
      }
    })
  }

  /**
   * get all the scoring made for a repository.
   *
   * @param repoName name of the repository to get the scores from ("owner/repo")
   * @return Seq of Scores.
   */
  def getFeedback(repoName: String): Future[Seq[Score]] = scoreDAO.findRepositoryScores(repoName)
}
