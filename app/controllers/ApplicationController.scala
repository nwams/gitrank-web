package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import models.User
import modules.CustomGitHubProvider
import play.api.i18n.MessagesApi

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
                                        gitHubProvider: CustomGitHubProvider)
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
   * @param repository repository name on the repo system. (GitHub)
   * @return The html page of the repository
   */
  def gitHubRepository(owner: String, repository: String) = UserAwareAction.async { implicit request =>
    Future.successful(Ok(views.html.repository(gitHubProvider, request.identity)(owner, repository)))
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
