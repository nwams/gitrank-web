package models.services

import com.google.inject.Inject
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
    contributionDAO.findAll(user.username).map(hm => { calculateKarma(user, hm)})
  }

  /**
   * Calculate karma based on the user contributions
   * @param user User to calculate
   * @param contributions Map of Repos/Contributions
   */

  def calculateKarma(user: User, contributions: HashMap[Repository,Contribution]): Int ={
    var score = 0.0
    contributions.foreach{
      case (key,value) => score = score + (((value.addedLines.toFloat+value.removedLines)/(key.addedLines+key.removedLines)))*(key.score*key.score)
    }
    score.toInt
  }


}
