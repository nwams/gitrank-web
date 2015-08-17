package models.daos.drivers

import javax.inject.Inject

import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import models.daos.OAuth2InfoDAO
import models.{Repository, User}
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GitHubAPI @Inject() (ws: WSClient, oauthDAO: OAuth2InfoDAO){

  val GITHUB_API_URL = "https://api.github.com"

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

  def getContributedRepositories(user: User, oauthInfo: OAuth2Info): Future[Seq[Repository]] = {
    val url = GITHUB_API_URL + "/users/" + user.username.get +"/events/public"
    val request: WSRequest = ws.url(url)
    println(url)

    buildGitHubReq(request, oauthInfo)
      .withHeaders("If-None-Match" -> user.publicEventsETag.getOrElse(""))
      .get()
      .map(response => {
        println("2. Response from the public timeline")
        println(response.json)
        Seq()
    })
  }
}
