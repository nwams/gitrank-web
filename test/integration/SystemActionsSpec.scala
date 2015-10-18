package integration

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import com.mohiva.play.silhouette.test.{FakeEnvironment, _}
import controllers.ApplicationController
import models.User
import models.daos.drivers.GitHubAPI
import models.services.{QuickstartService, RepositoryService, UserService}
import modules.CustomGitHubProvider
import org.specs2.specification._
import play.api.i18n.MessagesApi
import play.api.test._
import play.filters.csrf.CSRF
import setup.TestSetup

import scala.concurrent.ExecutionContext.Implicits.global


class SystemActionsSpec extends PlaySpecification with BeforeAfterEach {

  def before = TestSetup.populateNeo4JData()

  "RepositoryService " should {

    "be able to find repo with all feedbacks" in new WithApplication {
      app.injector.instanceOf[RepositoryService].getFeedback("test/test1",Option(1)).map{
        case feedbackList => feedbackList must not be empty
        }
      }

    "if repo does not exists, return empty feedback" in new WithApplication {
      app.injector.instanceOf[RepositoryService].getFeedback("elastic/elasticsearch",Option(1)).map{
        case feedbackList => feedbackList must be empty
      }
    }

  }
  def after = TestSetup.clearNeo4JData

}

