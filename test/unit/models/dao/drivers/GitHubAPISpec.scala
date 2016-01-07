package unit.models.dao.drivers

import models.daos.OAuth2InfoDAO
import models.daos.drivers.GitHubAPI
import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Context
import play.api.libs.ws.WSClient
import play.api.test.WithApplication
import play.api.Configuration
import com.typesafe.config.ConfigFactory

@RunWith(classOf[JUnitRunner])
class GitHubAPISpec extends Specification with Mockito {

  val ws = mock[WSClient]
  val oauthDAO = mock[OAuth2InfoDAO]
  val config = Configuration(ConfigFactory.load("application.test.conf"))

  "gitHubApi#parseGitHubLink" should {
    "return empty map if not paginated" in new WithApplication with Context{
      val gitHubApi = new GitHubAPI(oauthDAO, config)
      val map = gitHubApi.parseGitHubLink("")
      map must beEmpty
    }

    "return map with single if single page" in new WithApplication with Context{
      val gitHubApi = new GitHubAPI(oauthDAO, config)
      val map = gitHubApi.parseGitHubLink("name;url")
      map must not beEmpty;
      map must haveKey("url")
    }

    "return map with two values for double pages" in new WithApplication with Context {
      val gitHubApi = new GitHubAPI(oauthDAO, config)
      val map = gitHubApi.parseGitHubLink("name;url,name2;url2")
      map must not beEmpty;
      map must haveKey("url")
      map must haveKey("url2")
    }
  }
}
