package models.daos.drivers

import java.text.SimpleDateFormat
import javax.inject.Inject

import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import dispatch._
import models.daos.OAuth2InfoDAO
import models.{Contribution, GitHubRepo, Repository, User}
import org.apache.http.{HttpHeaders, HttpStatus}
import play.api.Configuration
import play.api.libs.json.{JsArray, JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class GitHubAPI @Inject()(oauthDAO: OAuth2InfoDAO,
                         configuration: Configuration) {

  val retryCount = 4
  val backoutTime = 1000l

  val gitHubApiUrl = configuration.getString("gitrank.githubApiUri").getOrElse("https://api.github.com")
  val gitHubDateFormatter = new SimpleDateFormat("yyyy-mm-dd'T'hh:mm:ss'Z'")
  val githubClientId = configuration.getString("silhouette.github.clientID").getOrElse("")
  val githubClientSecret = configuration.getString("silhouette.github.clientSecret").getOrElse("")

  /**
   * Get the user contributions statistics since last year or since the past update
   *
   * @param repositoryName name of the repository to get the statistics from
   * @param user user to get the statistics
   * @param oAuth2Info Authentication info
   * @return Contribution to be used as an update.
   */
  def getUserContribution(repositoryName: String, user: User, oAuth2Info: OAuth2Info): Future[Option[Contribution]] =
    doGitHubRequest(gitHubApiUrl + "/repos/" + repositoryName + "/stats/contributors", Some(oAuth2Info))
      .map({
        case None => None
        case Some(res) =>
          val userContribution = res.json.as[JsArray].value
            .filter(contributor => (contributor \ "author" \ "login").as[String] == user.username)

          userContribution.length match {
            case 0 => None
            case 1 => Some((userContribution.head \ "weeks").as[JsArray].value.foldRight(Contribution(0, 0, 0, None)) {
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

  /**
   * Retrieves a blank repository from GitHub.
   *
   * @param repositoryName full name of the repository to retrieve
   * @param oAuth2Info authentication information to use the api
   * @return A repository with all fields initiated except for the score and weight field that are initiated by default
   *         to 0, returns None if the repository was not found
   */
  def getRepository(repositoryName: String,
                    oAuth2Info: Option[OAuth2Info] = None,
                    retryCount: Int = retryCount): Future[Option[Repository]] = {

    doGitHubRequest(gitHubApiUrl + "/repos/" + repositoryName + "/stats/contributors", oAuth2Info)
      .flatMap({
        case None => Future.successful(None)
        case Some(res) =>
          val linesAdded = res.json.as[JsArray].value.foldLeft(0)(
            (accumulator: Int, contributor: JsValue) => {
              (contributor \ "weeks").as[JsArray].value.foldLeft(accumulator) {
                (innerAcc: Int, week: JsValue) => innerAcc + (week \ "a").as[Int]
              }
            })

          val linesDeleted = res.json.as[JsArray].value.foldLeft(0)(
            (accumulator: Int, contributor: JsValue) => {
              (contributor \ "weeks").as[JsArray].value.foldLeft(accumulator) {
                (innerAcc: Int, week: JsValue) => innerAcc + (week \ "d").as[Int]
              }
            })


          val innerReq = buildGitHubReq(gitHubApiUrl + "/repos/" + repositoryName, oAuth2Info)
            .addQueryParameter("client_id", githubClientId)
            .addQueryParameter("client_secret", githubClientSecret)

          Http(innerReq OK as.String)
            .map(body => Some(Repository((Json.parse(body) \ "id").as[Int], linesAdded, linesDeleted, 0, repositoryName, 0)))
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
  def getMostStaredRepositories(size: Int,
                                 oAuth2Info: Option[OAuth2Info]= None,

                                filters: Seq[String] = Seq()
                                 ): Future[Seq[GitHubRepo]] = {

    val query = "stars:\"> 1000\" " + filters.map(filter => "NOT \"" + filter + "\"").mkString(" ")

    val req = buildGitHubReq(gitHubApiUrl + "/search/repositories", oAuth2Info)
      .addQueryParameter("q", query)
      .addQueryParameter("sort", "stars")

    Http(req OK as.String)
      .map(body => (Json.parse(body) \ "items").as[Seq[GitHubRepo]].take(size))
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
    doUserStaredRepositoriesQuery(gitHubApiUrl + "/users/" + user.username + "/starred", size, user, oAuth2Info, filter)

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


    doGitHubRequest(url, Some(oAuth2Info))
      .flatMap({
        case Some(res) =>
          val repoList = res.json.as[Seq[GitHubRepo]]
            .filter(repo => !filter.contains(repo.name))
            .take(size)

          if (repoList.length < size && res.nextPage.isEmpty){
            getMostStaredRepositories(size - repoList.length, Some(oAuth2Info), filter)
              .map(publicRepos => publicRepos ++ repoList)
          } else if (repoList.length < size && res.nextPage.isDefined){
            doUserStaredRepositoriesQuery(res.nextPage.get, size - repoList.length, user, oAuth2Info, filter)
              .map(innerRepoList => repoList ++ innerRepoList)
          } else {
            Future.successful(repoList)
          }

        case None => getMostStaredRepositories(size, Some(oAuth2Info))
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
  def getContributedRepositories(user: User, oAuth2Info: OAuth2Info): Future[Set[String]] =
    doContributionRequest(gitHubApiUrl + "/users/" + user.username + "/events/public", user, oAuth2Info)

  /**
   * Actual implementation of the request and of the recursion.
   *
   * @param url url of the public user event timeline
   * @param user user you want to get the contributions from
   * @param oAuth2Info Authentication information of the user
   * @return Set of repository names
   */
  private def doContributionRequest(url: String, user: User, oAuth2Info: OAuth2Info): Future[Set[String]] =
    doGitHubRequest(url, Some(oAuth2Info))
      .flatMap({
        case None => Future.successful(Set())
        case Some(res) => res.nextPage match {
          case Some(page) => doContributionRequest(page, user, oAuth2Info).map((repoList: Set[String]) =>
            repoList.size match {
              case 0 => parseRepoNames(res.json, user.lastPublicEventPull)
              case _ => repoList ++ parseRepoNames(res.json, user.lastPublicEventPull)
            })
          case None => Future(parseRepoNames(res.json, user.lastPublicEventPull))
        }
      })

  /**
   * To be used to build a request to GitHub
   *
   * @param URL set URL to got to
   * @return A Ws Request populated with auth info.
   */
  private def buildGitHubReq(URL: String,
                             oauthInfo: Option[OAuth2Info] = None,
                             versionETag: String = ""): Req = {

    val req = url(URL)
      .addHeader(HttpHeaders.ACCEPT, "application/json ; charset=UTF-8")
      .addHeader(HttpHeaders.CONTENT_TYPE, "application/json")

    if (versionETag != "") {
      req.addHeader(HttpHeaders.IF_NONE_MATCH, versionETag)
    }

    oauthInfo match {
      case None => req
        .addQueryParameter("client_id", githubClientId)
        .addQueryParameter("client_secret", githubClientSecret)
      case Some(oAuth) =>req.addHeader(HttpHeaders.AUTHORIZATION, "token " + oAuth.accessToken)
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
      case str: String => (str.split(',') map { part: String =>
        val section = part.split(';')
        val url = section(0).replace("<", "").replace(">", "")
        val name = section(1).replace(" rel=\"", "").replace("\"", "")
        (name, url)
      }).toMap
      case _ => Map[String, String]()
    }
  }

  /**
   * Gets a set of all the repository names out of the response from the api. This set is filtered according to the
   * specified provided time. If no time is specified then it gets all the repository names.
   *
   * @param json Json to extract the name from
   * @param timeLimit Long representing the time in milliseconds from which to filter selection
   * @return a set of the repository names
   */
  private def parseRepoNames(json: JsValue, timeLimit: Option[Long]): Set[String] = {
    timeLimit match {
      case None => json.as[JsArray].value.map(event => (event \ "repo" \ "name").as[String]).toSet
      case Some(time) => json.as[JsArray].value
        .filter(event => gitHubDateFormatter.parse((event \ "created_at").as[String]).getTime > time)
        .map(event => (event \ "repo" \ "name").as[String])
        .toSet
    }
  }

  /**
    * Function that retries a github request if the result is that the request is correctly formated but the Github
    * cache is not hot. Returns the body content as Json
    *
    * @param url url of the request to make
    * @param oauth user Oauth, credentials to make the request
    * @param retryCount number of times to make the request, default 2
    * @return Json content of the response.
    */
  private def doGitHubRequest(url: String, oauth: Option[OAuth2Info], retryCount: Int = retryCount): Future[Option[GitHubResponse]] =
    Http(buildGitHubReq(url, oauth))
      .flatMap(response => {
        response.getStatusCode match {
          case HttpStatus.SC_OK =>
            val linkHeader = parseGitHubLink(response.getHeader("Link"))
            Future.successful(Some(GitHubResponse(
              url,
              linkHeader.get("next"),
              Json.parse(response.getResponseBody)
            )))
          case HttpStatus.SC_ACCEPTED =>
            if (retryCount > 0) {
              Thread.sleep(backoutTime)
              doGitHubRequest(url, oauth, retryCount - 1)
            } else {
              Future.successful(None)
            }
          case _ => Future.successful(None)
        }
      })
}
