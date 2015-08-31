package models.services

import javax.inject._

import actors.GitHubActor.UpdateContributions
import akka.actor.ActorRef
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models.daos.drivers.GitHubAPI
import models.daos.{ContributionDAO, OAuth2InfoDAO, ScoreDAO, UserDAO}
import models.{Score, User}
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
                                 userDAO: UserDAO,
                                 contributionDAO: ContributionDAO,
                                 scoreDAO: ScoreDAO,
                                 oAuth2InfoDAO: OAuth2InfoDAO,
                                 @Named("github-actor") gitHubActor: ActorRef) extends IdentityService[User]{

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
        println("Creating a new User")
        userDAO.create(User(
          loginInfo = profile.loginInfo,
          username = profile.username.get,
          fullName = profile.fullName,
          email = profile.email,
          avatarURL = profile.avatarURL,
          karma = 0,
          None,
          None
        )).map(user => {
          // We load the user contributions and update his karma in the background.
          println("Request to get the contributions to the actor")
          gitHubActor ! UpdateContributions(user, oAuth2Info)
          user
        })
    }
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
  def scoreRepository(username: String, repoName:String, score: Score): Future[Option[Score]] = scoreDAO.save(username, repoName, score)

  def propagateKarma (user: User) = ???

}
