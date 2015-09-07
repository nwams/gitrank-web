package models.services

import java.util.Date
import javax.inject.Inject

import models.{Score, Repository, User}
import models.daos.drivers.GitHubAPI
import models.daos.{ScoreDAO, UserDAO, ContributionDAO, RepositoryDAO}

import scala.concurrent.Future


class ScoreService @Inject() (scoreDAO: ScoreDAO) {

  /**
   * Gives a specific score to a repo.
   * @param user user logged in
   * @param repository repo to be scored
   * @param scoreDocumentation score given for documentation
   * @param scoreMaturity score given for maturity
   * @param scoreDesign score given for design
   * @param scoreSupport score given for support
   * @param feedback feedback written by user
   */
  def createScore(user: User,  repository: Repository, scoreDocumentation: Int, scoreMaturity: Int, scoreDesign: Int, scoreSupport: Int, feedback:String) : Repository = {
      scoreDAO.save(user.username, repository.name, createScore(user.karma,scoreDocumentation,scoreMaturity, scoreDesign, scoreSupport, feedback))
      repository
  }

  private def createScore(karma: Int,  scoreDocumentation: Int, scoreMaturity: Int, scoreDesign: Int, scoreSupport: Int, feedback:String): Score = {
    Score(
      new Date(),
      scoreDesign,
      scoreDocumentation,
      scoreSupport,
      scoreMaturity,
      feedback,
      karma
    )
  }
}
