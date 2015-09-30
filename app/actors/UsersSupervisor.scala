package actors


import javax.inject.Inject

import actors.UsersSupervisor.{AskGithubForUserContributions, SchedulePulling}
import akka.actor._
import akka.event.LoggingReceive
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models.User
import models.daos.{ContributionDAO, OAuth2InfoDAO, UserDAO}
import models.services.UserService
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.concurrent.duration._


object UsersSupervisor {
  case class AskGithubForUserContributions(loginInfo: LoginInfo)

  case class SchedulePulling(user: User)

  def props = Props[UsersSupervisor]
}

class UsersSupervisor @Inject()(
                                 userDAO: UserDAO,
                                 oAuth2InfoDAO: OAuth2InfoDAO,
                                 contributionDAO: ContributionDAO,
                                 userService: UserService) extends Actor with ActorLogging {

  var cancellableUpdates: Map[LoginInfo, Cancellable] = Map.empty[LoginInfo, Cancellable]

  /**
   * When starting the supervisor, we fetch all users in database and scheduleUpdates
   */
  override def preStart(): Unit = {
    userDAO.findAll(callbackUsersProcessor)
  }

  override def receive: Receive = LoggingReceive {
    case s: String => log.info(s)
    case AskGithubForUserContributions(loginInfo) =>
      for {
        user: Option[User] <- userDAO.find(loginInfo)
        authInfo: Option[OAuth2Info] <- oAuth2InfoDAO.find(loginInfo)
      } yield userService.updateContributions(user.get, authInfo.get)
    case SchedulePulling(user) =>
      addCancellableWithReplacement(schedulePullingFromGithub(user))
  }

  /**
   * this is scheduling to send an UpdateUser to the UsersSupervisor every hour starting now.
   * @param user
   */
  private def schedulePullingFromGithub(user: User): (LoginInfo, Cancellable) = {
    user.loginInfo -> context.system.scheduler.schedule(0.microseconds, 1.hour, self, AskGithubForUserContributions(user.loginInfo))
  }

  /**
   * Responsible for the callback from the streaming of users from the database, scheduling updates.
   * @param user Each user returned on the stream
   */
  def callbackUsersProcessor(user: Any): Future[Unit] = Future{ addCancellableWithReplacement(schedulePullingFromGithub(user.asInstanceOf[User]))}

  /**
   * cancel previous schedule if exists and add new one to cancellableUpdates
   * @param loginInfoWithCancellable
   */
  private def addCancellableWithReplacement(loginInfoWithCancellable: (LoginInfo, Cancellable)) =
    loginInfoWithCancellable match {
      case tuple@(loginInfo, cancellable) =>
        cancellableUpdates.get(loginInfo).map(_.cancel())
        cancellableUpdates += tuple
    }
}