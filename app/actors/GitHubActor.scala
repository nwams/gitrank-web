package actors

import javax.inject.Inject

import akka.actor._
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models.User
import models.daos.drivers.GitHubAPI
import models.services.UserService

import scala.concurrent.ExecutionContext.Implicits.global

object GitHubActor {
  def props = Props[GitHubActor]

  case class UpdateContributions(user: User, oAuth2Info: OAuth2Info)
}

class GitHubActor @Inject() (userService: UserService, gitHubAPI: GitHubAPI) extends Actor {
  import GitHubActor._

  def receive = {
    case UpdateContributions(user: User, oAuth2Info: OAuth2Info) => {
      gitHubAPI.getContributedGitHubRepositories(user, oAuth2Info).map({
        case Some(repositories) => {
          for (repositoryName <- repositories){
            gitHubAPI.getUserContribution(repositoryName, user, oAuth2Info).map({
              case Some(contrib) => println(contrib.toString)
              case None => println("no contribution found")
            })
          }
        }
        case None => None
      })
    }
  }
}
