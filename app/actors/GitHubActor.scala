package actors

import javax.inject.Inject

import akka.actor._
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models.{Contribution, Repository, User}
import models.daos.drivers.GitHubAPI
import models.services.{RepositoryService, UserService}

import scala.concurrent.ExecutionContext.Implicits.global

object GitHubActor {
  def props = Props[GitHubActor]

  case class UpdateContributions(user: User, oAuth2Info: OAuth2Info)
}

class GitHubActor @Inject()(
                             userService: UserService,
                             repoService: RepositoryService,
                             gitHubAPI: GitHubAPI
                           ) extends Actor {
  import GitHubActor._

  def receive = {
    case UpdateContributions(user: User, oAuth2Info: OAuth2Info) =>
      gitHubAPI.getContributedRepositories(user, oAuth2Info)
        .map(repositoryNames => {
          for (repositoryName <- repositoryNames){
            gitHubAPI.getUserContribution(repositoryName, user, oAuth2Info).map({
              case None => None
              case Some(contribution: Contribution) => gitHubAPI.getRepository(repositoryName, Some(oAuth2Info)).map(
                (opt: Option[Repository])=> opt match {
                  case None => None
                  case Some(repository: Repository) => repoService.save(
                    repository.repoID,
                    repositoryName,
                    Some(repository.addedLines),
                    Some(repository.removedLines),
                    None,
                    None
                  ).map(repo => repoService.saveContribution(user.username, repositoryName, contribution))
              })
            })
          }
      })
  }
}
