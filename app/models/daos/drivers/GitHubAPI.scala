package models.daos.drivers

import java.text.SimpleDateFormat
import javax.inject.Inject

import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models.daos.OAuth2InfoDAO
import models.{Contribution, User}
import play.api.libs.json.{JsArray, JsValue}
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GitHubAPI @Inject() (ws: WSClient, oauthDAO: OAuth2InfoDAO){

  val GITHUB_API_URL = "https://api.github.com"
  val GITHUB_DATE_FORMATTER = new SimpleDateFormat("yyyy-mm-dd'T'hh:mm:ss'Z'")

  /**
   * To be used to build a request to GitHub
   *
   * @param req set request with the URL to go to
   * @return A Ws Request populated with auth info.
   */
  def buildGitHubReq(req: WSRequest, oauthInfo: OAuth2Info): WSRequest = req
    .withHeaders(
      "Accept" -> "application/json ; charset=UTF-8",
      "Content-Type" -> "application/json",
      "Authorization" -> ("token "+ oauthInfo.accessToken))
    .withRequestTimeout(10000)

  /**
   * Get the user contributions statistics since last year or since the past update
   *
   * @param repositoryName name of the repository to get the statistics from
   * @param user user to get the statistics
   * @param oAuth2Info Authentication info
   * @return Contribution to be used as an update.
   */
  def getUserContribution(repositoryName: String, user: User, oAuth2Info: OAuth2Info): Future[Option[Contribution]] = {
    val request = ws.url(GITHUB_API_URL + "/repos/" + repositoryName + "/stats/contributors")
    buildGitHubReq(request, oAuth2Info)
      .get()
      .map(response => {
      val userContribution = response.json.as[JsArray].value
        .filter(contributor => (contributor \ "author" \ "login").as[String] == user.username.getOrElse(""))
      userContribution.length match {
        case 0 => None
        case 1 => {
          Some((userContribution.head \ "weeks").as[JsArray].value.foldRight(Contribution(0, 0, 0, None)){
            (value: JsValue, contribution: Contribution) => {
              Contribution(
                (value \ "w").as[Long],
                contribution.addedLines + (value \ "a").as[Int],
                contribution.removedLines + (value \ "d").as[Int],
                Some("a" + (value \ "a").as[Int] + "d" + (value \ "d").as[Int])
              )
            }
          })
        }
      }
    })
  }

  /**
   * Get the repository name set of the repository a user has contributed to. This method explores the GitHub api
   * pages recursively to get all the contributions. The GitHub API is limited to 10 pages with 30 events per pages
   * with a 90 days limit to the api. This is the best we can do using the direct API.
   *
   * @param user user you want to get the contributions from
   * @param oAuth2Info Authentication information of the user
   * @return
   */
  def getContributedGitHubRepositories(user: User, oAuth2Info: OAuth2Info): Future[Option[Set[String]]] =
    doContributionRequest(GITHUB_API_URL + "/users/" + user.username.get +"/events/public", user, oAuth2Info)

  /**
   * Actual implementation of the request and of the recursion.
   *
   * @param url url of the public user event timeline
   * @param user user you want to get the contributions from
   * @param oAuth2Info Authentication information of the user
   * @return Set of repository names
   */
  private def doContributionRequest(url: String, user: User, oAuth2Info: OAuth2Info): Future[Option[Set[String]]] = {
    val request: WSRequest = ws.url(url)
    buildGitHubReq(request, oAuth2Info)
      .withHeaders("If-None-Match" -> user.publicEventsETag.getOrElse(""))
      .get()
      .flatMap(response => {
      response.status match {
        case 304 => Future(None)
        case 200 => {
          val linkHeader = parseGitHubLink(response.header("Link").getOrElse(""))
          if (linkHeader.isDefinedAt("next")) {
            doContributionRequest(linkHeader.getOrElse("next", ""), user, oAuth2Info).map({
              case Some(repoList) => Some(repoList ++ parseRepoNames(response, user.lastPublicEventPull))
              case None => Some(parseRepoNames(response, user.lastPublicEventPull))
            })
          } else {
            Future(Some(parseRepoNames(response, user.lastPublicEventPull)))
          }
        }
      }
    })
  }

  /**
   * Parses a link String from GitHub to get the purposes and the links out of it
   *
   * @param linkHeader String with the links inside
   * @return Map of the keys with their links.
   */
  private def parseGitHubLink(linkHeader: String): Map[String, String] = {
    (linkHeader.split(',') map { part: String =>
      val section = part.split(';')
      val url = section(0).replace("<", "").replace(">", "")
      val name = section(1).replace(" rel=\"", "").replace("\"", "")
      (name, url)
    }).toMap
  }

  /**
   * Gets a set of all the repository names out of the response from the api. This set is filtered according to the
   * specified provided time. If no time is specified then it gets all the repository names.
   *
   * @param wSResponse response to extract the repository names from
   * @param timeLimit Long representing the time in milliseconds from which to filter selection
   * @return a set of the repository names
   */
  private def parseRepoNames(wSResponse: WSResponse, timeLimit: Option[Long]): Set[String] = {
    timeLimit match {
      case None => wSResponse.json.as[JsArray].value.map(event => (event \ "repo" \ "name").as[String]).toSet
      case Some(time) => wSResponse.json.as[JsArray].value
        .filter(event => {
          GITHUB_DATE_FORMATTER.parse((event \ "created_at").as[String]).getTime > time
        })
        .map(event => (event \ "repo" \ "name").as[String])
        .toSet
    }
  }

}
