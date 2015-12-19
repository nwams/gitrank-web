package actors

import javax.inject.Inject

import actors.RepositorySupervisor.PropagateRepositoryScore
import akka.actor.{Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import models.daos.UserDAO
import models.services.{RepositoryService, UserService}
import models.{Repository, Score}

import scala.concurrent.ExecutionContext.Implicits.global

object RepositorySupervisor {

  case class PropagateRepositoryScore(repository: Repository, score: Score)

  def props = Props[RepositorySupervisor]
}

class RepositorySupervisor @Inject()(
                                      userDAO: UserDAO,
                                      userService: UserService,
                                      repositoryService: RepositoryService
                                    ) extends Actor with ActorLogging {

  override def receive: Receive = LoggingReceive {
    case s: String => log.info(s)
    case PropagateRepositoryScore(repository, score) =>
      for {
        users <- userDAO.findAllFromRepo(repository)
      } yield users.foreach(userService.propagateKarma)
  }
}