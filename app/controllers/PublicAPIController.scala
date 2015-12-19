package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import models.User
import models.services.{ElasticSearchService, QuickstartService, RepositoryService}
import play.api.libs.json._
import play.api.mvc.Action
import play.api.i18n.MessagesApi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Controller for the public APIs endpoints
 *
 * @param repoService Repository service injected
 */
class PublicAPIController @Inject()(
                                     val messagesApi: MessagesApi,
                                     val env: Environment[User, SessionAuthenticator],
                                     repoService: RepositoryService,
                                     quickstartService: QuickstartService,
                                     elasticSearchService: ElasticSearchService
                                     )
  extends Silhouette[User, SessionAuthenticator] {

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
  def getScoreBadge(owner: String, repoName: String) = Action.async { implicit request =>
    repoService.retrieve(owner + "/" + repoName).map({
      case Some(repo) => Ok(buildScoreBadge(repo.score)).as("image/svg+xml")
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
  def getGuides(owner: String, repositoryName: String) = UserAwareAction.async { implicit request =>
    val repoName: String = owner + "/" + repositoryName
    repoService.getFromNeoOrGitHub(None, repoName).flatMap({
      case Some(repository) =>
        quickstartService.getQuickstartGuidesForRepo(repository).map(guides =>
          Ok(Json.toJson(guides))
        )
      case None => Future(NotFound("No Quickstart guide found"))
    })
  }

  /**
    * API endpoint to delete a quickstart guide
    *
    * @param id of the quickstart to be deleted
    * @return OK or NotModified
    */
  def deleteQuickstart(id: Int) = SecuredAction.async { implicit  request =>
    quickstartService.delete(request.identity, id).map({
      case true => Ok
      case false => NotModified
    })
  }

  /**
   * Service for searching repo names
   *
   * @param query query to search for repo
   * @return the json list of repo names
   */
  def searchRepos(query: String) = Action.async { implicit request =>
    elasticSearchService.searchForRepos(query).map(
      repoNames => repoNames.isEmpty match {
        case _ => Ok(Json.toJson(Map("results" -> repoNames)))
      }
    )
  }

  /**
   * Build the svg file for the score repo badge
   *
   * @param repoScore score of the repository. Should be a float between 0 and 5.
   * @return svg file.
   */
  private def buildScoreBadge(repoScore: Float) = {

    val color = repoScore match {
      case score if score >= 4 => "#4c1"
      case score if score >= 3 => "#97CA00"
      case score if score >= 2 => "#dfb317"
      case score if score >= 1 => "#fe7d37"
      case score if score >= 0 => "#e05d44"
      case _ => "#555"
    }

    // Taken from shield.io template
    <svg xmlns="http://www.w3.org/2000/svg" width="117" height="20">
      <linearGradient id="b" x2="0" y2="100%">
        <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
        <stop offset="1" stop-opacity=".1"/>
      </linearGradient>
      <mask id="a">
        <rect width="117" height="20" rx="3" fill="#fff"/>
      </mask>
      <g mask="url(#a)">
        <path fill="#555" d="M0 0h89v20H0z"/>
        <path fill={color} d="M89 0h28v20H89z"/>
        <path fill="url(#b)" d="M0 0h117v20H0z"/>
      </g>
      <g fill="#fff" text-anchor="middle" font-family="DejaVu Sans,Verdana,Geneva,sans-serif" font-size="11">
        <text x="44.5" y="15" fill="#010101" fill-opacity=".3">Gitrank Score</text>
        <text x="44.5" y="14">Gitrank Score</text>
        <text x="102" y="15" fill="#010101" fill-opacity=".3">
          {repoScore}
        </text>
        <text x="102" y="14">
          {repoScore}
        </text>
      </g>
    </svg>
  }
}
