package actors

import javax.inject.{Named, Inject}

import akka.actor.Actor.Receive
import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import models.{Score, Repository}
import models.daos.{RepositoryDAO, ContributionDAO, OAuth2InfoDAO, UserDAO}
import models.services.UserService

object RepositorySupervisor {

  case class ScoreRepository(repository: Repository, score: Score)

  def props = Props[RepositorySupervisor]

}
class RepositorySupervisor @Inject()(repositoryDAO: RepositoryDAO) extends Actor with ActorLogging {


  override def receive: Receive = ???
}