package controllers

import javax.inject.Inject

import models.services.RepositoryService
import play.api.libs.json._
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Controller for the public APIs endpoints
 *
 * @param repoService Repository service injected
 */
class PublicAPIController @Inject() (
                                repoService: RepositoryService
                              )
  extends Controller {

  /**
   * API point used by the parallel coordinates to get the scoring data.
   *
   * @param owner owner of the repository
   * @param repoName name of the repository
   * @return
   */
  def getLast100Feedback(owner: String, repoName:String) = Action.async { implicit request =>
    repoService.getScores(owner + "/" + repoName, 1, 100).map(scores => Ok(Json.toJson(scores)))
  }
}
