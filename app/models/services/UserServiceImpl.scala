package models.services

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import models.{Contribution, User}
import models.daos.{ContributionDAO, UserDAO}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

/**
 * Handles actions to users.
 *
 * @param userDAO The user DAO implementation.
 */
class UserServiceImpl @Inject() (userDAO: UserDAO, contributionDAO: ContributionDAO) extends UserService {

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
  def save(user: User) = userDAO.save(user)

  /**
   * Saves the social profile for a user.
   *
   * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
   *
   * @param profile The social profile to save.
   * @return The user for whom the profile was saved.
   */
  def save(profile: CommonSocialProfile) = {
    userDAO.find(profile.loginInfo).flatMap {
      case Some(user) => // Update user with profile
        userDAO.save(user.copy(
          fullName = profile.fullName,
          email = profile.email,
          avatarURL = profile.avatarURL
        ))
      case None => // Insert a new user
        userDAO.save(User(
          userID = UUID.randomUUID(),
          loginInfo = profile.loginInfo,
          username = Some(""),
          fullName = profile.fullName,
          email = profile.email,
          avatarURL = profile.avatarURL,
          karma = 0
        ))
    }
  }

  /**
   * Adds a contribution for a user to a repository. If the user has already contributed to the repository, it adds
   * the new added lines and removed lines to the existing contribution and updates the timestamp.
   *
   * @param username name of the contributing user
   * @param repoName name of the repository he contributes to.
   * @param contribution contribution to be saved.
   * @return contribution to be saved or none
   */
   def addContribution(username: String, repoName: String, contribution: Contribution): Future[Option[Contribution]] = {
    contributionDAO.find(username, repoName).flatMap {
      case Some(oldContribution) =>
        contributionDAO.save(username, repoName, contribution.copy(
          timestamp = contribution.timestamp,
          addedLines = contribution.addedLines + oldContribution.addedLines,
          removedLines = contribution.removedLines + oldContribution.removedLines
        ))
      case None => contributionDAO.add(username, repoName, contribution)
    }
  }
}
