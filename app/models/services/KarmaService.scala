package models.services

import com.google.inject.Inject
import models.daos.{ContributionDAO, RepositoryDAO, UserDAO}
import models.{Contribution, Repository, User}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class KarmaService @Inject()(userDAO: UserDAO, repositoryDAO: RepositoryDAO, contributionDAO: ContributionDAO){


  /**
   * Propagate User Karma, updating it if needed.
   *
   * @param user User to propagate karma
   */
  def propagateUserKarma(user: User): Future[Unit] =
    contributionDAO.findAll(user.username).map { contrib => updateUser(user, calculateKarma(user, contrib))}

  /**
   * Calculate karma based on the user contributions
   *
   * @param user User to calculate
   * @param contributions Map of Repos/Contributions
   */
  def calculateKarma(user: User, contributions: Seq[(Repository,Contribution)]): Int ={
    if(contributions.length==0) 0
    else{
      (contributions.map{
        case (key,value) => ((value.addedLines.toFloat+value.removedLines)/(key.addedLines+key.removedLines))*(key.score*key.score)
      }.sum.toInt)/contributions.length}
  }

  /**
   * Update User with new karma score
   *
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
