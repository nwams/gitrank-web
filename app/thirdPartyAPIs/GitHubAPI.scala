package thirdPartyAPIs

import javax.inject.Inject

import models.daos.OAuth2InfoDAO
import models.Identifiable
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GitHubAPI @Inject() (ws: WSClient, oauthDAO: OAuth2InfoDAO){

  val GITHUB_API_URL = "https://api.github.com"

  /**
   * To be used to build a request to GitHub
   *
   * @param req set request with the URL to go to
   * @param user user with a LoginInfo Property.
   * @return A Ws Request populated with auth info.
   */
  def buildGitHubReq(req: WSRequest, user: Identifiable): Future[WSRequest] = {
    oauthDAO.find(user.loginInfo).map {
      case None => throw new Exception("Trying to make a request to GitHub without having an OAuth token");
      case Some(oauthInfo) => {
        req.withHeaders(
          "Accept" -> "application/json ; charset=UTF-8",
          "Content-Type" -> "application/json",
          "Authorization" -> ("token "+ oauthInfo.accessToken)
        ).withRequestTimeout(10000)
      }
    }
  }
}
