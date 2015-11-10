package models.services

import javax.inject._

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models.daos.drivers.{NeoParsers, GitHubAPI}
import models.daos._
import models.{Contribution, Repository, Score, User}
import modules.CustomSocialProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Handles actions to users.
 *
 * @param userDAO The user DAO implementation.
 */
@Singleton
class UserService @Inject() (gitHubAPi: GitHubAPI,
                                 parser: NeoParsers,
                                 userDAO: UserDAO,
                                 contributionDAO: ContributionDAO,
                                 scoreDAO: ScoreDAO,
                                 oAuth2InfoDAO: OAuth2InfoDAO,
                                 gitHubAPI: GitHubAPI,
                                 repoDAO: RepositoryDAO,
                                 karmaService: KarmaService) extends IdentityService[User]{

  /**
   * Retrieves a user that matches the specified login info.
   *
   * @param loginInfo The login info to retrieve a user.
   * @return The retrieved user or None if no user could be retrieved for the given login info.
   */
  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = userDAO.find(loginInfo)

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User): Future[User] = userDAO.update(user)

  /**
   * Saves the social profile for a user.
   *
   * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
   *
   * @param profile The social profile to save.
   * @return The user for whom the profile was saved.
   */
  def save(profile: CustomSocialProfile, oAuth2Info: OAuth2Info) : Future[User] = {
    userDAO.find(profile.loginInfo).flatMap {
      case Some(user) => // Update user with profile
        userDAO.update(user.copy(
          fullName = profile.fullName,
          email = profile.email,
          avatarURL = profile.avatarURL
        ))
      case None => // Insert a new use
        userDAO.create(User(
          loginInfo = profile.loginInfo,
          username = profile.username,
          fullName = profile.fullName,
          email = profile.email,
          avatarURL = profile.avatarURL,
          karma = 1,
          None,
          None
        )).flatMap(user => {
          // We load the user contributions and update his karma in the background.
          updateContributions(user, oAuth2Info).map(s=> user)
        })
    }
  }

  /**
   * Update the contributions from the github api server. This method does not use the other services to prevent
   * a dependency cycle
   *
   * @param user User that should be gotten the contributions from
   * @param oAuth2Info Github authentication information
   */
  def updateContributions(user: User, oAuth2Info: OAuth2Info): Future[Unit] = {
    gitHubAPI.getContributedRepositories(user, oAuth2Info)
      .map(repositoryNames => {
      for (repositoryName <- repositoryNames){
        gitHubAPI.getUserContribution(repositoryName, user, oAuth2Info).map({
          case None => None
          case Some(contribution: Contribution) => gitHubAPI.getRepository(repositoryName, Some(oAuth2Info)).map(
            (opt: Option[Repository])=> opt match {
              case None => None
              case Some(repository: Repository) => repoDAO.find(repository.repoID).flatMap({
                case Some(existingRepo) =>
                  repoDAO.update(existingRepo.copy(
                    name = repositoryName,
                    addedLines = repository.addedLines,
                    removedLines = repository.removedLines
                  ))
                case None =>
                  repoDAO.create(Repository(
                    repoID = repository.repoID,
                    addedLines =repository.addedLines,
                    removedLines = repository.removedLines,
                    karmaWeight = 0,
                    name = repositoryName,
                    score = 0
                  ))
              }).map(repo => contributionDAO.find(user.username, repositoryName).flatMap({
                case Some(existingContribution) =>
                  contributionDAO.update(user.username, repositoryName, existingContribution.copy(
                    timestamp = contribution.timestamp,
                    addedLines = existingContribution.addedLines + contribution.addedLines -
                      parser.parseWeekAddedLines(existingContribution.currentWeekBuffer),
                    removedLines = existingContribution.removedLines + contribution.removedLines -
                      parser.parseWeekDeletedLines(existingContribution.currentWeekBuffer),
                    currentWeekBuffer = contribution.currentWeekBuffer
                  ))
                case None => contributionDAO.add(user.username, repositoryName, contribution)
              }))
            })
        })
      }
    })
  }

  /**
   * Gets the Oauth information of a user
   *
   * @param user user to get the Oauth information for
   * @return OAuth2Info
   */
  def getOAuthInfo(user: User): Future[Option[OAuth2Info]] = oAuth2InfoDAO.find(user.loginInfo)

  /**
   * Adds a score to the repository from the given user
   *
   * @param username username of the user that scored the repository
   * @param repoName name of the repository that was scored
   * @param score score to be saved.
   * @return saved score.
   */
  def scoreRepository(username: String, repoName:String, score: Score): Future[Option[Score]] =
    scoreDAO.save(username, repoName, score)

  /**
   * Updates the user's karma according to its contributions
   *
   * @param user user to update the karma
   * @return
   */
  def propagateKarma (user: User) = karmaService.propagateUserKarma(user)

  /**
    * Gets the list of all the names of the repos the user has already scored
    *
    * @param user to get the score from
    * @return Sequence of repository names
    */
  def getScoredRepositoriesNames(user: User): Future[Seq[String]] =
    userDAO.findScoredRepositories(user).map(_.map(repo => repo.name))
}
