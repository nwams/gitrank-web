package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import models.daos.drivers.GitHubAPI
import models.services.{RepositoryService, UserService}
import models.{Repository, User}
import modules.CustomGitHubProvider
import play.api.i18n.MessagesApi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * The basic application controller.
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 * @param gitHubProvider The social provider registry.
 */
class ApplicationController @Inject() (
                                        val messagesApi: MessagesApi,
                                        val env: Environment[User, SessionAuthenticator],
                                        gitHubProvider: CustomGitHubProvider,
                                        repoService: RepositoryService,
                                        userService: UserService,
                                        gitHub: GitHubAPI)
  extends Silhouette[User, SessionAuthenticator] {

  /**
   * Handles the main action.
   *
   * @return The result to display.
   */
  def index = UserAwareAction.async { implicit request =>
    Future.successful(Ok(views.html.home(gitHubProvider, request.identity)))
  }

  /**
   * Handles the repository view
   *
   * @param owner Owner of the repository on the repo system (GitHub)
   * @param repositoryName repository name on the repo system. (GitHub)
   * @return The html page of the repository
   */
  def gitHubRepository(owner: String, repositoryName: String) = UserAwareAction.async { implicit request =>
    repoService.retrieve(owner + "/" + repositoryName).flatMap((repoOption: Option[Repository]) => repoOption match {
      case Some(repository) => Future.successful(Ok(views.html.repository(gitHubProvider, request.identity, repository)(owner, repositoryName)))
      case None => (request.identity match {
        case None => gitHub.getRepository(owner + "/" + repositoryName)
        case Some(user) => userService.getOAuthInfo(user).flatMap(oAuthInfo => gitHub.getRepository(owner + "/" + repositoryName, oAuthInfo))
      }).map({
        case None => NotFound(views.html.error(
          "notFound",404 , "Not Found", "We cannot find the requested page, try something else !"
        ))
        case Some(repo: Repository) => Ok(views.html.repository(gitHubProvider, request.identity, repo)(owner, repositoryName))
      })
    })
  }

  /**
   * Handles the feedback page
   *
   * @param owner Owner of the repository on the repo system (GitHub)
   * @param repository repository name on the repo system (GitHub)
   * @return the hml page with the scoring form for the given repository.
   */
  def giveFeedbackPage(owner: String, repository: String) = UserAwareAction.async {implicit request =>
    Future.successful(Ok(views.html.feedbackForm(gitHubProvider, request.identity)(owner, repository)))
  }
}
