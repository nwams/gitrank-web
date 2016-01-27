package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import forms.FeedbackForm
import models.User
import models.daos.drivers.GitHubAPI
import models.forms.QuickstartForm
import models.services.{QuickstartService, RepositoryService, UserService}
import modules.CustomGitHubProvider
import org.apache.http.HttpStatus
import play.api.Play
import play.api.Play.current
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
class ApplicationController @Inject()(
                                       val messagesApi: MessagesApi,
                                       val env: Environment[User, SessionAuthenticator],
                                       gitHubProvider: CustomGitHubProvider,
                                       repoService: RepositoryService,
                                       userService: UserService,
                                       gitHub: GitHubAPI,
                                       quickstartService: QuickstartService)
  extends Silhouette[User, SessionAuthenticator] {


  /**
   * Handles the main action.
   *
   * @return The result to display.
   */
  def index = UserAwareAction.async { implicit request =>
    val repoCount = Play.configuration.getInt("gitrank.homeReposSuggestions").getOrElse(0)

    request.identity match {
      case Some(user) => userService.getOAuthInfo(user).flatMap({
        case Some(oauthInfo) => userService.getScoredRepositoriesNames(user).flatMap(filter =>
          gitHub.getUserStaredRepositories(repoCount, user, oauthInfo, filter).map(gitHubRepos =>
            Ok(views.html.home(gitHubProvider, request.identity, gitHubRepos))
          )
        )
        case None => throw new Error("User spotted without OAuth Credentials: " + user.username)
      })
      case None => gitHub.getMostStaredRepositories(repoCount).map(gitHubRepos =>
        Ok(views.html.home(gitHubProvider, request.identity, gitHubRepos))
      )
    }
  }

  /**
   * Handles the repository view
   *
   * @param owner Owner of the repository on the repo system (GitHub)
   * @param repositoryName repository name on the repo system. (GitHub)
   * @return The html page of the repository
   */
  def gitHubRepository(owner: String,
                       repositoryName: String,
                       feedbackPage: Option[Int] = None,
                       quickstartPage: Option[Int] = None
                      ) = UserAwareAction.async { implicit request =>

    if (feedbackPage.getOrElse(1) <= 0 || quickstartPage.getOrElse(1) <= 0) {
      Future.successful(NotFound(views.html.error("notFound", HttpStatus.SC_NOT_FOUND, "Not Found",
        "We cannot find the page, unfortunately negative pages have not been invented!"))
      )
    } else {
      val repoName: String = owner + "/" + repositoryName
      repoService.getFromNeoOrGitHub(request.identity, repoName).flatMap({
        case Some(repository) =>

          val futureFeedback = repoService.getFeedback(repoName, feedbackPage)
          val futureQuickstart = quickstartService.getQuickstartGuidesForRepo(repository, quickstartPage)
          val futureFeedbackPageCount = repoService.getFeedbackPageCount(repoName)
          val futureQuickstartPageCount = quickstartService.getQuickstartPageCount(repoName)

          futureFeedback.flatMap(feedback =>
            futureFeedbackPageCount.flatMap(totalFeedbackPages =>
              futureQuickstart.flatMap(quickstart =>
                futureQuickstartPageCount.flatMap(totalQuickstartPages =>

                  if (totalFeedbackPages == 0 || feedbackPage.getOrElse(1) <= totalFeedbackPages ||
                    totalQuickstartPages == 0 || quickstartPage.getOrElse(1) <= totalQuickstartPages
                  ) {
                    repoService.canAddFeedback(repoName, request.identity).flatMap({
                      case true => repoService.canUpdateFeedback(repoName, request.identity).map(
                        canUpdate => Ok(views.html.repository(
                          gitHubProvider,
                          request.identity,
                          repository,
                          feedback,
                          quickstart,
                          totalFeedbackPages,
                          totalQuickstartPages,
                          true,
                          canUpdate
                        )(owner, repositoryName, feedbackPage.getOrElse(1), quickstartPage.getOrElse(1))))
                      case false => Future.successful(Ok(views.html.repository(
                        gitHubProvider,
                        request.identity,
                        repository,
                        feedback,
                        quickstart,
                        totalFeedbackPages,
                        totalQuickstartPages
                      )(owner, repositoryName, feedbackPage.getOrElse(1), quickstartPage.getOrElse(1))))
                    })
                  } else {
                    Future.successful(NotFound(views.html.error("notFound", HttpStatus.SC_NOT_FOUND, "Not Found",
                      "The requested page does not exist")))
                  }
                )
              )
            )
          )

        case None => Future.successful(NotFound(views.html.error("notFound", HttpStatus.SC_NOT_FOUND, "Not Found",
          "We cannot find the repository page, it is likely that you misspelled it, try something else!")))
      })
    }
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
    repoService.getFromNeoOrGitHub(request.identity, repoName).flatMap({
      case Some(repository) =>
        request.identity match {
          case Some(id) => repoService.canAddFeedback(repoName, request.identity).flatMap{
            case canAdd => canAdd match {
              case false => Future.successful(Redirect(routes.ApplicationController.gitHubRepository(
                owner,
                repositoryName,
                None,
                None
              ).url))
              case true => repoService.canUpdateFeedback(repoName, request.identity).flatMap(canUpdate =>
                repoService.getMapScoreFromUser(repoName,request.identity).map(map =>
                  map.isEmpty match {
                    case false => Ok(views.html.feedbackForm(gitHubProvider, request.identity)(owner, repositoryName, FeedbackForm.form.bind(map), canUpdate))
                    case true => Ok(views.html.feedbackForm(gitHubProvider, request.identity)(owner, repositoryName, FeedbackForm.form, canUpdate))
                  }
                ))
            }
          }
          case None => Future.successful(Ok(views.html.feedbackForm(gitHubProvider, request.identity)(owner, repositoryName, FeedbackForm.form, false)))
        }


      case None => Future.successful(NotFound(views.html.error("notFound", HttpStatus.SC_NOT_FOUND, "Not Found",
        "We cannot find the repository feedback page, it is likely that you misspelled it, try something else!")))
    })
  }

  /**
   * Handles the feedback score post
   *
   * @param owner Owner of the repository on the repo system (GitHub)
   * @param repositoryName repository name on the repo system (GitHub)
   * @return Redirect to repo page
   */
  def postScore(owner: String, repositoryName: String, update: Option[Boolean]) = SecuredAction.async { implicit request =>
    FeedbackForm.form.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.feedbackForm(gitHubProvider, Some(request.identity))
        (owner, repositoryName, formWithErrors, update.getOrElse(false)))),
      data => repoService.giveScoreToRepo(owner,
        request.identity,
        repositoryName,
        data.scoreDocumentation,
        data.scoreMaturity,
        data.scoreDesign,
        data.scoreSupport,
        data.feedback
      ).map(repo => Redirect(routes.ApplicationController.gitHubRepository(
        owner,
        repositoryName,
        None,
        None
      ).url))
    )
  }

  /**
   * Handles the quickstart guide post
   *
   * @param owner Owner of the repository on the repo system (GitHub)
   * @param repositoryName repository name on the repo system (GitHub)
   * @return Redirect to repo page
   */
  def postQuickstartGuide(owner: String, repositoryName: String) = SecuredAction.async { implicit request =>
    QuickstartForm.form.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.quickstartGuide(gitHubProvider, Some(request.identity))
        (owner, repositoryName, formWithErrors)))
      ,
      data => quickstartService.createQuickstart(
        request.identity,
        owner + "/" + repositoryName,
        data.title,
        data.description,
        QuickstartForm.validateUrl(data.url)
      ).map(q => Redirect(routes.ApplicationController.gitHubRepository(
        owner,
        repositoryName,
        None,
        None
      ).url))
    )
  }

  /**
   * Handles the feedback page
   *
   * @param owner Owner of the repository on the repo system (GitHub)
   * @param repositoryName repository name on the repo system (GitHub)
   * @return the hml page with the scoring form for the given repository.
   */
  def createGuidePage(owner: String, repositoryName: String) = UserAwareAction.async { implicit request =>
    val repoName: String = owner + "/" + repositoryName
    repoService.getFromNeoOrGitHub(request.identity, repoName).map({
      case Some(repository) =>
        Ok(views.html.quickstartGuide(gitHubProvider, request.identity)(owner, repositoryName, QuickstartForm.form))
      case None => NotFound(views.html.error("notFound", HttpStatus.SC_NOT_FOUND, "Not Found",
        "We cannot find the repository feedback page, it is likely that you misspelled it, try something else!"))
    })
  }

  /**
   * Service for upvoting a guide
   *
   * @param owner Owner of the repository on the repo system (GitHub)
   * @param repositoryName repository name on the repo system (GitHub)
   * @param id id of the guide
   * @param voteType if the vote is upvote or downvote
   * @return the guide
   */
  def upVote(owner: String, repositoryName: String, id: Int, voteType: String) = SecuredAction.async { implicit request =>
    val repoName: String = owner + "/" + repositoryName
    repoService.getFromNeoOrGitHub(Some(request.identity), repoName).flatMap({
      case Some(repository) =>
        voteType match {
          case "upvote" => quickstartService.updateVote(repository, true, id, request.identity)
            .map({
            case Some(guide) => Redirect(routes.ApplicationController.gitHubRepository(
              owner,
              repositoryName,
              None,
              None
            ).url)
            case None => NotFound(views.html.error("notFound", HttpStatus.SC_NOT_FOUND, "Not Found",
              "We cannot find the guide."))
          })
          case "downvote" => quickstartService.updateVote(repository, false, id, request.identity)
            .map({
            case Some(guide) => Redirect(routes.ApplicationController.gitHubRepository(
              owner,
              repositoryName,
              None,
              None
            ).url)
            case None =>
              NotFound(views.html.error("notFound", HttpStatus.SC_NOT_FOUND, "Not Found",
                "We cannot find the guide, it is likely that you misspelled it, try something else!"))
          })
          case _ => Future.successful(
            MethodNotAllowed(views.html.error("methodNotAllowed", HttpStatus.SC_METHOD_NOT_ALLOWED, "Not Allowed",
            "Trying some funny stuff, the incident will be reported"))
          )
        }
      case None => Future.successful(NotFound(views.html.error("notFound", HttpStatus.SC_NOT_FOUND, "Not Found",
        "We cannot find the repository feedback page, it is likely that you misspelled it, try something else!")))
    })
  }
}
