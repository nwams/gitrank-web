package integration

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import com.mohiva.play.silhouette.test.{FakeEnvironment, _}
import controllers.ApplicationController
import models.User
import models.daos.RepositoryDAO
import models.daos.drivers.GitHubAPI
import models.services.{QuickstartService, RepositoryService, UserService}
import modules.CustomGitHubProvider
import org.specs2.specification._
import play.api.i18n.MessagesApi
import play.api.test._
import play.filters.csrf.CSRF
import setup.TestSetup

import scala.concurrent.ExecutionContext.Implicits.global


class RepositoryServiceIntegrationSpec extends PlaySpecification with BeforeAfterEach {

  def before = TestSetup.populateNeo4JData()

  "RepositoryService " should {


    "be able to find repo with all feedbacks" in new WithApplication {
      app.injector.instanceOf[RepositoryService].getFeedback("test/test1", Option(1)).map {
        case feedbackList => feedbackList must not be empty
      }
    }

    "if repo does not exists, return empty feedback" in new WithApplication {
      app.injector.instanceOf[RepositoryService].getFeedback("elastic/elasticsearch", Option(1)).map {
        case feedbackList => feedbackList must beEmpty
      }
    }
    "not allow user to add feedback if he has already done it" in new WithApplication() {
      val identity = User(LoginInfo("github", "test@test.com"),
        "User1",
        Option("userFullName"),
        Option("test@test.com"),
        None,
        1,
        None,
        None
      )
      app.injector.instanceOf[RepositoryService].canAddFeedback("test/test0", Option(identity)) map {
        case true => failure
        case false => success
      }
    }
    " allow user to add feedback if he has not already done it" in new WithApplication() {
      val identity = User(LoginInfo("github", "test@test.com"),
        "User1234",
        Option("userFullName"),
        Option("test@test.com"),
        None,
        1,
        None, // An ETag is used to know if the data has been modified since the last poll
        None
      )
      app.injector.instanceOf[RepositoryService].canAddFeedback("test/test0", Option(identity)).map {
        case true => success
        case false => failure
      }
    }
    "find repo even if it does not exist on database" in new WithApplication() {
      app.injector.instanceOf[RepositoryService].getFromNeoOrGitHub(None, "elastic/elasticsearch").map {
        optionRepo => optionRepo match {
          case Some(repo) => repo.name must be equalTo ("elastic/elasticsearch")
          case None => failure
        }
      }
    }
    "create repo if not on database" in new WithApplication() {
      val identity = User(LoginInfo("github", "test@test.com"),
        "User1",
        Option("userFullName"),
        Option("test@test.com"),
        None,
        1,
        None,
        None
      )
      app.injector.instanceOf[RepositoryService].findOrCreate(identity, "elastic/elasticsearch").map {
        optionRepo =>  app.injector.instanceOf[RepositoryDAO].find(optionRepo.name).map(
          r => r must not be(None)
        )
      }
    }


    def after = TestSetup.clearNeo4JData

  }
}

