package models.dao.drivers

import models.daos.OAuth2InfoDAO
import models.daos.drivers.GitHubAPI
import org.junit.runner.RunWith
import org.mockito.MockSettings
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.libs.json.{JsArray, JsValue}
import play.api.libs.ws.{WSResponse, WSClient}

import scala.collection.generic.CanBuildFrom

/**
 * Created by brunnoattorre1 on 8/31/15.
 */

@RunWith(classOf[JUnitRunner])
class GitHubAPISpec extends  Specification with Mockito {
  val ws = mock[WSClient]
  val oauthDAO = mock[OAuth2InfoDAO]
  val gitHubApi = new GitHubAPI(ws, oauthDAO )

  "gitHubApi#parseGitHubLink" should {
    "return empty map if not paginated" in {
      val map = gitHubApi.parseGitHubLink("")
      map must beEmpty
    }
    "return map with single if single page" in {
      val map = gitHubApi.parseGitHubLink("name;url")
      map must not beEmpty;
      map must haveKey("url")
    }
    "return map with two values for double pages" in {
      val map = gitHubApi.parseGitHubLink("name;url,name2;url2")
      map must not beEmpty;
      map must haveKey("url")
      map must haveKey("url2")

    }
  }


}
