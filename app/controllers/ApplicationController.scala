package controllers

import javax.inject.Inject

import akka.actor.FSM.Failure
import akka.actor.Status.Success
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import models.daos.drivers.GitHubAPI
import models.services.{RepositoryService, UserService}
import models.{Feedback, User, Repository}
import modules.CustomGitHubProvider
import play.api.i18n.MessagesApi
import play.api.mvc.{Result, Action, Results, WrappedRequest}
import forms.FeedbackForm
import views.html.feedbackForm

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
 * The basic application controller.
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 * @param gitHubProvider The social provider registry.
 */
class ApplicationController @Inject()(
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
  def gitHubRepository(owner: String, repositoryName: String, page: Option[Int]) = UserAwareAction.async { implicit request =>
    val repoName: String = owner + "/" + repositoryName
    repoService.getFromNeoOrGitHub(request.identity, repoName).flatMap({
      case Some(repository) => repoService.getFeedback(repoName, page).flatMap((feedback: Seq[Feedback]) =>
        repoService.getFeedbackPageCount(repoName).flatMap(totalPage => {
          repoService.getPermissionToAddFeedback(repoName, request.identity).map (
             permission => Ok(views.html.repository(gitHubProvider, request.identity, repository, feedback, totalPage, permission)(owner, repositoryName, page.getOrElse(1)))
          )
        })
      )
      case None => Future(NotFound(views.html.error("notFound", 404, "Not Found",
        "We cannot find the repository page, it is likely that you misspelled it, try something else !")))
    })
  }

  /**
   * Handles the feedback page
   *
   * @param owner Owner of the repository on the repo system (GitHub)
   * @param repositoryName repository name on the repo system (GitHub)
   * @return the hml page with the scoring form for the given repository.
   */
  def giveFeedbackPage(owner: String, repositoryName: String) = UserAwareAction.async { implicit request =>
    val repoName: String = owner + "/" + repositoryName
    repoService.getPermissionToAddFeedback(repoName, request.identity).flatMap {
      case true => repoService.getFromNeoOrGitHub(request.identity,repoName).flatMap({
        case Some(repository) =>
          repoService.getPermissionToAddFeedback(repoName, request.identity).map {
            permission => Ok(views.html.feedbackForm(gitHubProvider, request.identity)(owner, repositoryName, FeedbackForm.form, permission))
          }
        case None => Future(NotFound(views.html.error("notFound", 404, "Not Found",
          "We cannot find the repository feedback page, it is likely that you misspelled it, try something else !")))
      })
      case false => Future.successful(Redirect(routes.ApplicationController.gitHubRepository(owner, repositoryName, None).url))
    }
  }

  /**
   * Handles the feedback score post
   *
   * @param owner Owner of the repository on the repo system (GitHub)
   * @param repositoryName repository name on the repo system (GitHub)
   * @return Redirect to repo page
   */
  def giveScorePage(owner: String, repositoryName: String) = UserAwareAction.async { implicit request =>
    FeedbackForm.form.bindFromRequest.fold(
      form => println(form),
      data=>{
      request.identity.map(repoService.giveScoreToRepo(owner,
        _,
        repositoryName,
        data.scoreDocumentation,
        data.scoreMaturity,
        data.scoreDesign,
        data.scoreSupport,
        data.feedback
      ))
    })
    Future.successful(Redirect(routes.ApplicationController.gitHubRepository(owner, repositoryName, None).url))
  }

}