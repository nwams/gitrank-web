package models.services

import javax.inject.Inject

import models.daos.drivers.GitHubAPI
import models.daos.{ScoreDAO, ContributionDAO, RepositoryDAO, UserDAO}
import models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RepositoryService @Inject()(
                                   repoDAO: RepositoryDAO,
                                   contributionDAO: ContributionDAO,
                                   userDAO: UserDAO,
                                   scoreDAO: ScoreDAO,
                                   gitHub: GitHubAPI,
                                   userService: UserService,
                                   scoreService: ScoreService) {

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
        contributionDAO.update(userName, repoName, existingContribution.copy(
          timestamp = contribution.timestamp,
          addedLines = existingContribution.addedLines + contribution.addedLines - parseWeekAddedLines(existingContribution.currentWeekBuffer),
          removedLines = existingContribution.removedLines + contribution.removedLines - parseWeekDeletedLines(existingContribution.currentWeekBuffer),
          currentWeekBuffer = contribution.currentWeekBuffer
        ))
      }
      case None => contributionDAO.add(userName, repoName, contribution)
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
  def findContributors(repoName: String): Future[Seq[User]] = {
    repoDAO.find(repoName).flatMap({
      case Some(repository) => userDAO.findAllFromRepo(repository)
      case None => Future(Seq())
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
  def getFromNeoOrGitHub(identity: Option[User], repoName: String): Future[Option[Repository]] = {
    retrieve(repoName).flatMap((repoOption: Option[Repository]) => repoOption match {
      case Some(repository) => Future(Some(repository))
      case None => identity match {
        case None => gitHub.getRepository(repoName)
        case Some(user) => userService.getOAuthInfo(user).flatMap(oAuthInfo => gitHub.getRepository(repoName, oAuthInfo))
      }
    })
  }

  /**
   * get all the feedback made for a repository for the given page and item per page.
   *
   * @param repoName name of the repository to get the scores from ("owner/repo")
   * @param page page number to get from the database. Default value to 1
   * @param itemsPerPage number of items to display in a database page
   * @return Seq of Feedback.
   */
  def getFeedback(repoName: String, page: Option[Int], itemsPerPage: Int = 10): Future[Seq[Feedback]] = page match {
    case Some(p) => scoreDAO.findRepositoryFeedback(repoName, p, itemsPerPage)
    case None => scoreDAO.findRepositoryFeedback(repoName, 1, itemsPerPage)
  }

  /**
   * get all the scoring made for a repository for the given page and item per page.
   *
   * @param repoName name of the repository to get the scores from ("owner/repo")
   * @param page page number to get from the database. Default value to 1
   * @param itemsPerPage number of items to display in a database page
   * @return Seq of Scores.
   */
  def getScores(repoName: String, page: Int=1, itemsPerPage: Int =10): Future[Seq[Score]] =
    scoreDAO.findRepositoryScores(repoName, page, itemsPerPage)

  /**
   * Check if the user can add or just update a score
   *
   * @param repoName Repo to be scored
   * @param user User that is giving the score
   * @return true if the user can add
   */
  def getPermissionToAddFeedback(repoName: String, user: Option[User]): Future[Boolean] = {
    user match {
      case Some(userEntity) => scoreDAO.find(userEntity.username, repoName).map(_.isEmpty)
      case None => Future.successful(false)
    }
  }

  /**
   * Gets the number of feedback page result for a given repository.
   *
   * @param repoName name of the repository to get the page count from
   * @param itemsPerPage number of items to put in the page
   * @return number of page as an integer.
   */
  def getFeedbackPageCount(repoName: String, itemsPerPage: Int = 10): Future[Int] = {

    if (itemsPerPage == 0) {
      throw new Exception("There can't be 0 items on a page")
    }

    scoreDAO.countRepositoryFeedback(repoName).map(feedbackCount => {
      feedbackCount % itemsPerPage match {
        case 0 => feedbackCount / itemsPerPage
        case _ => (feedbackCount / itemsPerPage) + 1
      }
    })
  }

  /**
   * Gives a specific score to a repo.
   *
   * @param owner User logged in
   * @param repositoryName name of the repo to be scored
   * @param scoreDocumentation score given for documentation
   * @param scoreMaturity score given for maturity
   * @param scoreDesign score given for design
   * @param scoreSupport score given for support
   * @param feedback feedback written by user
   * @return repo scored
   */
  def giveScoreToRepo(owner: String, user: User, repositoryName: String, scoreDocumentation: Int, scoreMaturity: Int, scoreDesign: Int, scoreSupport: Int, feedback: String): Future[Repository] = {
    repoDAO.find(owner + "/" + repositoryName).map({
      case Some(repo) => scoreService.createScore(user, repo, scoreDocumentation, scoreMaturity, scoreDesign, scoreSupport, feedback)
      case None => throw new Exception("Repository does not exists!")
    })
  }

  /**
   * Recalculate score for repo
   * @param repository repo to recalculate
   */
  def calculateScoreForRepo(repository: Repository): Future[(Int)] = {
    scoreDAO.findRepositoryFeedback(repository.name).map {
      _.map(feedback => (repository.karmaWeight * repository.score + feedback.user.karma * computeScore(feedback.score))
        / (repository.karmaWeight + feedback.user.karma)
      ).sum
    }
  }

  /**
   * Compute the mean of the scores between all the scores
   *
   * @param score a score composed of 4 scores and a feedback text
   * @return a mean
   */
  private def computeScore(score: Score): Int = {
    (score.designScore + score.docScore + score.maturityScore + score.supportScore )/4
  }

  /**
   * Update a score of a given repo
   *
   * @param repository repo to update
   */
  def updateRepoScore(repository: Repository): Future[Future[Repository]] = {
    calculateScoreForRepo(repository).map(
      score => Repository(
        repoID = repository.repoID,
        addedLines = repository.addedLines,
        removedLines = repository.removedLines,
        karmaWeight = repository.karmaWeight,
        name = repository.name,
        score = score)
    ).map(repoDAO.update)
  }
}
