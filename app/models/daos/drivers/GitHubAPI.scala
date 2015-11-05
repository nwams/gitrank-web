package models.daos.drivers

import java.text.SimpleDateFormat
import javax.inject.Inject

import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models.daos.OAuth2InfoDAO
import models.{GitHubRepo, Contribution, Repository, User}
import play.api.Play
import play.api.Play.current
import play.api.libs.json.{JsArray, JsValue}
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GitHubAPI @Inject() (ws: WSClient, oauthDAO: OAuth2InfoDAO){

  val gitHubApiUrl = Play.configuration.getString("gitrank.githubApiUri").getOrElse("https://api.github.com")
  val gitHubDateFormatter = new SimpleDateFormat("yyyy-mm-dd'T'hh:mm:ss'Z'")
  val githubClientId = Play.configuration.getString("silhouette.github.clientID").getOrElse("")
  val githubClientSecret = Play.configuration.getString("silhouette.github.clientSecret").getOrElse("")


  /**
   * Get the user contributions statistics since last year or since the past update
   *
   * @param repositoryName name of the repository to get the statistics from
   * @param user user to get the statistics
   * @param oAuth2Info Authentication info
   * @return Contribution to be used as an update.
   */
  def getUserContribution(repositoryName: String, user: User, oAuth2Info: OAuth2Info): Future[Option[Contribution]] = {
    buildGitHubReq(ws.url(gitHubApiUrl + "/repos/" + repositoryName + "/stats/contributors"), Some(oAuth2Info))
      .get()
      .map(response => {
      val userContribution = response.json.as[JsArray].value
        .filter(contributor => (contributor \ "author" \ "login").as[String] == user.username)
      userContribution.length match {
        case 0 => None
        case 1 => Some((userContribution.head \ "weeks").as[JsArray].value.foldRight(Contribution(0, 0, 0, None)){
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
    })
  }

  /**
   * Retrieves a blank repository from GitHub.
   *
   * @param repositoryName full name of the repository to retrieve
   * @param oAuth2Info authentication information to use the api
   * @return A repository with all fields initiated except for the score and weight field that are initiated by default
   *         to 0, returns None if the repository was not found
   */
  def getRepository(repositoryName: String, oAuth2Info: Option[OAuth2Info] = None): Future[Option[Repository]] = {
    buildGitHubReq(ws.url(gitHubApiUrl + "/repos/" + repositoryName + "/stats/contributors"), oAuth2Info)
      .get()
      .flatMap(response => {
      response.status match {
        case 200 =>
          val linesAdded = response.json.as[JsArray].value.foldLeft(0)((accumulator: Int, contributor: JsValue) => {
            (contributor \ "weeks").as[JsArray].value.foldLeft(accumulator){
              (innerAcc: Int, week: JsValue) => innerAcc + (week \ "a").as[Int]
            }
          })

          val linesDeleted = response.json.as[JsArray].value.foldLeft(0)((accumulator: Int, contributor: JsValue) => {
            (contributor \ "weeks").as[JsArray].value.foldLeft(accumulator){
              (innerAcc: Int, week: JsValue) => innerAcc + (week \ "d").as[Int]
            }
          })

          buildGitHubReq(ws.url(gitHubApiUrl + "/repos/" + repositoryName).withQueryString(("client_id",githubClientId),("client_secret",githubClientSecret)), oAuth2Info)
            .get()
            .map(response => {
            Some(Repository((response.json \ "id").as[Int], linesAdded, linesDeleted, 0, repositoryName, 0))
          })
        case _ => Future(None)
      }
    })
  }

  /**
    * Gets the most stared repositories from GitHub.
    *
    * @param size How many repositories do we want, should be less than a 100 (GitHub page limit)
    * @param oAuth2Info Oauth information of the current user
    * @param filters filters the result by excluding the String list from the search results
    * @return a Sequence of GitHub repositories
    */
  def getMostStaredRepositories( size: Int,
                                 oAuth2Info: Option[OAuth2Info]= None,
                                filters: Seq[String] = Seq()
                               ): Future[Seq[GitHubRepo]] = {

    val query = "stars:\"> 1000\" " + filters.map(filter => "NOT \""+filter+"\"").mkString(" ")

    buildGitHubReq(ws.url(gitHubApiUrl + "/search/repositories"), oAuth2Info)
      .withQueryString(
        ("q", query),
        ("sort", "stars"))
      .get()
      .map(response => {
        response.status match {
          case 200 => (response.json \ "items").as[Seq[GitHubRepo]].take(size)
          case _ => Seq()
        }
      })
  }

  /**
    * Public API for making the request to GitHub to populate the homepage when the user is connected
    *
    * @param size number of item we want
    * @param user user requesting the content
    * @param oAuth2Info auth information of the current user
    * @param filter list of the repo names that we do not want to be in the result
    * @return
    */
  def getUserStaredRepositories(size: Int,
                                user: User,
                                oAuth2Info: OAuth2Info,
                                filter: Seq[String] = Seq()
                               ): Future[Seq[GitHubRepo]] =
    doUserStaredRepositoriesQuery(gitHubApiUrl + "/users/" + user.username + "/starred",size, user, oAuth2Info, filter)

  /**
    * This is a method that makes the query to get the user stared repository. This is a recursive function made
    * so to use the link header of the GitHub API if necessary to go through the pages
    *
    * @param url url to make the request to. (user starred url)
    * @param size size of the sequence of repo to return
    * @param user user that is making the request
    * @param oAuth2Info auth information about the connected user
    * @param filter filter containing the repo name we want to exclude from the research.
    * @return
    */
  private def doUserStaredRepositoriesQuery(
                                             url: String,
                                             size: Int,
                                             user: User,
                                             oAuth2Info: OAuth2Info,
                                             filter: Seq[String] = Seq()
                                           ): Future[Seq[GitHubRepo]] = {

    buildGitHubReq(ws.url(url), Some(oAuth2Info))
      .get()
      .flatMap(response =>
        response.status match {
          case 200 =>
            val linkHeader = parseGitHubLink(response.header("link").getOrElse(""))
            val repoList = response.json.as[Seq[GitHubRepo]]
              .filter(repo => !filter.contains(repo.name))
              .take(size)

            if (repoList.length < size && !linkHeader.isDefinedAt("next")){
              getMostStaredRepositories(size - repoList.length, Some(oAuth2Info), filter)
                .map(publicRepos => publicRepos ++ repoList)
            } else if (repoList.length < size && linkHeader.isDefinedAt("next")){
              doUserStaredRepositoriesQuery(linkHeader.get("next").get, size - repoList.length, user, oAuth2Info, filter)
                .map(innerRepoList => repoList ++ innerRepoList)
            } else {
              Future.successful(repoList)
            }

          case _ => getMostStaredRepositories(size, Some(oAuth2Info))
        }
      )
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
  def getContributedRepositories(user: User, oAuth2Info: OAuth2Info): Future[Set[String]] =
    doContributionRequest(gitHubApiUrl + "/users/" + user.username +"/events/public", user, oAuth2Info)

  /**
   * Actual implementation of the request and of the recursion.
   *
   * @param url url of the public user event timeline
   * @param user user you want to get the contributions from
   * @param oAuth2Info Authentication information of the user
   * @return Set of repository names
   */
  private def doContributionRequest(url: String, user: User, oAuth2Info: OAuth2Info): Future[Set[String]] = {
    buildGitHubReq(ws.url(url), Some(oAuth2Info))
      .withHeaders("If-None-Match" -> user.publicEventsETag.getOrElse(""))
      .get()
      .flatMap(response => {
      response.status match {
        case 304 => Future(Set())
        case 200 => {
          val linkHeader = parseGitHubLink(response.header("Link").getOrElse(""))
          if (linkHeader.isDefinedAt("next")) {
            doContributionRequest(linkHeader.getOrElse("next", ""), user, oAuth2Info).map((repoList: Set[String]) =>
            repoList.size match {
              case 0 => parseRepoNames(response, user.lastPublicEventPull)
              case _ => repoList ++ parseRepoNames(response, user.lastPublicEventPull)
            })
          } else {
            Future(parseRepoNames(response, user.lastPublicEventPull))
          }
        }
      }
    })
  }

  /**
   * To be used to build a request to GitHub
   *
   * @param req set request with the URL to go to
   * @return A Ws Request populated with auth info.
   */
  private def buildGitHubReq(req: WSRequest, oauthInfo: Option[OAuth2Info] = None): WSRequest = {
    req
      .withHeaders(
        "Accept" -> "application/json ; charset=UTF-8",
        "Content-Type" -> "application/json")
      .withRequestTimeout(10000)

    oauthInfo match {
      case None => req.withQueryString(
          ("client_id", githubClientId),
          ("client_secret", githubClientSecret)
        )
      case Some(oAuth) =>req.withHeaders("Authorization" -> ("token "+ oAuth.accessToken))
    }
  }

  /**
   * Parses a link String from GitHub to get the purposes and the links out of it
   *
   * @param linkHeader String with the links inside
   * @return Map of the keys with their links.
   */
  def parseGitHubLink(linkHeader: String): Map[String, String] = {
    linkHeader match {
      case "" => Map[String, String]()
      case _ => (linkHeader.split(',') map { part: String =>
        val section = part.split(';')
        val url = section(0).replace("<", "").replace(">", "")
        val name = section(1).replace(" rel=\"", "").replace("\"", "")
        (name, url)
      }).toMap
    }
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
        .filter(event => gitHubDateFormatter.parse((event \ "created_at").as[String]).getTime > time)
        .map(event => (event \ "repo" \ "name").as[String])
        .toSet
    }
  }
}
