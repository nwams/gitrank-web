package controllers

import javax.inject.Inject

import models.services.{QuickstartService, RepositoryService}
import play.api.libs.json._
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Controller for the public APIs endpoints
 *
 * @param repoService Repository service injected
 */
class PublicAPIController @Inject()(
                                     repoService: RepositoryService,
                                     quickstartService: QuickstartService
                                     )
  extends Controller {

  /**
   * API point used by the parallel coordinates to get the scoring data.
   *
   * @param owner owner of the repository
   * @param repoName name of the repository
   * @return
   */
  def getLast100Feedback(owner: String, repoName: String) = Action.async { implicit request =>
    repoService.getScores(owner + "/" + repoName, 1, 100).map(scores => Ok(Json.toJson(scores)))
  }

  /**
   * Service for getting the quickstart guides of a repo
   *
   * @param owner Owner of the repository on the repo system (GitHub)
   * @param repositoryName repository name on the repo system (GitHub)
   * @return the list of guides for the given repo
   */
  def getGuides(owner: String, repositoryName: String) = Action.async { implicit request =>
    val repoName: String = owner + "/" + repositoryName
    repoService.getFromNeoOrGitHub(None, repoName).flatMap({
      case Some(repository) =>
        quickstartService.getQuickstartGuidesForRepo(repository).map(guides =>
          Ok(Json.toJson(guides))
        )
      case None => Future(NotFound(views.html.error("notFound", 404, "Not Found",
        "We cannot find the repository feedback page, it is likely that you misspelled it, try something else !")))
    })
  }
}
