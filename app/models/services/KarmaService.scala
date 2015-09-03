package models.services

import com.google.inject.Inject
import com.mohiva.play.silhouette.api.LoginInfo
import models.daos.{ContributionDAO, RepositoryDAO, UserDAO}
import models.{Contribution, Repository, User}

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class KarmaService @Inject()(userDAO: UserDAO, repositoryDAO: RepositoryDAO, contributionDAO: ContributionDAO){


  /**
   * Propagate User Karma, updating it if needed.
   * @param user User to propagate karma
   */
  def propagateUserKarma(user: User): Future[Unit] ={
    contributionDAO.findAll(user.username).map {
      case contrib => updateUser(user, calculateKarma(user, contrib))
    }

  }

  /**
   * Calculate karma based on the user contributions
   * @param user User to calculate
   * @param contributions Map of Repos/Contributions
   */

  def calculateKarma(user: User, contributions: mutable.HashMap[Repository,Contribution]): Int ={
    var score = 0.0
    contributions.foreach{
      case (key,value) => score = score + (((value.addedLines.toFloat+value.removedLines)/(key.addedLines+key.removedLines)))*(key.score*key.score)
    }
    score.toInt
  }
  /**
   * Update User with new karma score
   * @param user User to update
   * @param karma karma score
   */
  def updateUser(user: User, karma: Int): Unit ={
    userDAO.update(User(
      user.loginInfo,
      user.username,
      user.fullName,
      user.email,
      user.avatarURL,
      karma,
      user.publicEventsETag,
      user.lastPublicEventPull
    ))
  }

}
