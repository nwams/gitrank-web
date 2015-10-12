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
   * Delivers a badge for the given repository
   *
   * @param owner name of the repository owner
   * @param repoName name of the repository
   * @return svg file with the score embeded and the right color
   */
  def getScoreBadge(owner: String, repoName:String) = Action.async { implicit request =>
    repoService.retrieve(owner+"/"+repoName).map({
      case Some(repo) =>

        val color = repo.score match {
          case score if score >= 4 => "#4c1"
          case score if score >= 3 => "#97CA00"
          case score if score >= 2 => "#dfb317"
          case score if score >= 1 => "#fe7d37"
          case score if score >= 0 => "#e05d44"
        }

        // Taken from shield.io template
        Ok(<svg xmlns="http://www.w3.org/2000/svg" width="118" height="20">
          <linearGradient id="b" x2="0" y2="100%">
            <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
            <stop offset="1" stop-opacity=".1"/>
          </linearGradient>
          <mask id="a">
            <rect width="118" height="20" rx="3" fill="#fff"/>
          </mask>
          <g mask="url(#a)">
            <path fill="#555" d="M0 0h89v20H0z"/>
            <path fill={color} d="M89 0h29v20H89z"/>
            <path fill="url(#b)" d="M0 0h118v20H0z"/>
          </g>
          <g fill="#fff" text-anchor="middle" font-family="DejaVu Sans,Verdana,Geneva,sans-serif" font-size="11">
            <text x="44.5" y="15" fill="#010101" fill-opacity=".3">Gitrank Score</text>
            <text x="44.5" y="14">Gitrank Score</text>
            <text x="102.5" y="15" fill="#010101" fill-opacity=".3">{repo.score}/5</text>
            <text x="102.5" y="14">4/5</text>
          </g>
        </svg>).as("image/svg+xml")
      case None => NotFound("Badge Not Found")
    })
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
