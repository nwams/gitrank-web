package actors

import javax.inject.{Named, Inject}

import actors.GitHubActor.UpdateContributions
import actors.RepositorySupervisor.ScoreRepository
import akka.actor.{ActorRef, Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models.daos.{OAuth2InfoDAO, UserDAO}
import models.services.{RepositoryService, UserService}
import models.{Repository, Score, User}
import scala.concurrent.ExecutionContext.Implicits.global
object RepositorySupervisor {

  case class ScoreRepository(repository: Repository, score: Score)

  def props = Props[RepositorySupervisor]

}
class RepositorySupervisor @Inject()(userDAO: UserDAO, userService: UserService, repositoryService: RepositoryService) extends Actor with ActorLogging {


  override def receive: Receive = LoggingReceive {
    case s: String => log.info(s)
    case ScoreRepository(repository, score) => {
      print("MESSAGE RECEIVED!!!")
      repositoryService.updateRepoScore(repository)
      for {
        users <- userDAO.findAllFromRepo(repository)
      } yield users.foreach(userService.propagateKarma)
    }
  }
}