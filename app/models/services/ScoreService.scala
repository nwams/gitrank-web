package models.services

import java.util.Date
import javax.inject.{Inject, Named, Singleton}

import actors.RepositorySupervisor.ScoreRepository
import akka.actor.ActorRef
import models.daos.ScoreDAO
import models.{Repository, Score, User}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ScoreService @Inject()(scoreDAO: ScoreDAO, @Named("repository-supervisor") repositorySupervisor: ActorRef) {

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
  def createScore(user: User,
                  repository: Repository,
                  scoreDocumentation: Int,
                  scoreMaturity: Int,
                  scoreDesign: Int,
                  scoreSupport: Int,
                  feedback: String): Repository = {
    val score = Score(
      new Date(),
      scoreDesign,
      scoreDocumentation,
      scoreSupport,
      scoreMaturity,
      feedback,
      user.karma
    )
    scoreDAO.find(user.username, repository.name).map {
      case Some(_) => scoreDAO.update(user.username, repository.name, score).foreach(_=>repositorySupervisor ! ScoreRepository(repository, score))
      case None => scoreDAO.save(user.username, repository.name, score).foreach(_=>repositorySupervisor ! ScoreRepository(repository, score))
    }
    repository
  }
}
