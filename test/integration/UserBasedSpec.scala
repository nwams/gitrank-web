package integration

import com.google.inject.Guice
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.authenticators.{SessionAuthenticator, CookieAuthenticator}
import com.mohiva.play.silhouette.test.{FakeEnvironment, _}
import controllers.ApplicationController
import models.User
import models.daos.{RepositoryDAO, ScoreDAO, UserDAO}
import models.daos.drivers.GitHubAPI
import models.services.{ScoreService, QuickstartService, RepositoryService, UserService}
import modules.CustomGitHubProvider
import org.specs2.matcher._
import org.specs2.mock.mockito.MockitoStubs
import org.specs2.specification._
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test._
import play.filters.csrf.CSRF
import setup.TestSetup
import scala.concurrent.ExecutionContext.Implicits.global


class UserBasedSpec extends PlaySpecification {

  "User Logged In " should {

    "be able to see the button to submit feedback to repo" in new WithApplication {
      val identity = User(LoginInfo("github", "test@test.com"),
        "User1",
        Option("userFullName"),
        Option("test@test.com"),
        None,
        1,
        None, // An ETag is used to know if the data has been modified since the last poll
        None
      )
      // Used to filter the already retrieved events)
      val messagesApi = app.injector.instanceOf[MessagesApi]
      implicit val env = FakeEnvironment[User, SessionAuthenticator](Seq(
        identity.loginInfo -> identity
      ))
      val request = FakeRequest().withAuthenticator(identity.loginInfo)
      val controller = new ApplicationController(messagesApi, env,
        app.injector.instanceOf[CustomGitHubProvider],
        app.injector.instanceOf[RepositoryService],
        app.injector.instanceOf[UserService],
        app.injector.instanceOf[GitHubAPI],
        app.injector.instanceOf[QuickstartService])

      val result = controller.gitHubRepository("elastic", "elasticsearch", None).apply(request)
      status(result) must equalTo(OK)

      val body = new String(contentAsBytes(result))
      body must contain("elasticsearch")

    }

    "be able to see the Add feedbakck page" in new WithApplication {
      val identity = User(LoginInfo("github", "test@test.com"),
        "User1",
        Option("userFullName"),
        Option("test@test.com"),
        None,
        1,
        None, // An ETag is used to know if the data has been modified since the last poll
        None
      )
      // Used to filter the already retrieved events)
      val messagesApi = app.injector.instanceOf[MessagesApi]
      implicit val env = FakeEnvironment[User, SessionAuthenticator](Seq(
        identity.loginInfo -> identity
      ))
      val request = FakeRequest().withAuthenticator(identity.loginInfo).withSession("csrfToken" -> CSRF.SignedTokenProvider.generateToken)
      val controller = new ApplicationController(messagesApi, env,
        app.injector.instanceOf[CustomGitHubProvider],
        app.injector.instanceOf[RepositoryService],
        app.injector.instanceOf[UserService],
        app.injector.instanceOf[GitHubAPI],
        app.injector.instanceOf[QuickstartService])

      val result = controller.giveFeedbackPage("elastic", "elasticsearch").apply(request)
      status(result) must equalTo(OK)

      val body = new String(contentAsBytes(result))
      body must contain("elasticsearch")
      body must contain("<button id=\"submit\" type=\"submit\"")

    }


    "be able to submit feedback" in new WithApplication {
      val identity = User(LoginInfo("github", "test@test.com"),
        "User1",
        Option("userFullName"),
        Option("test@test.com"),
        None,
        1,
        None,
        None
      )


      // Used to filter the already retrieved events)
      val messagesApi = app.injector.instanceOf[MessagesApi]
      implicit val env = FakeEnvironment[User, SessionAuthenticator](Seq(
        identity.loginInfo -> identity
      ))
      val request = FakeRequest().withAuthenticator(identity.loginInfo).withSession("csrfToken" -> CSRF.SignedTokenProvider.generateToken).withFormUrlEncodedBody(("scoreDocumentation", "2"), ("scoreMaturity", "3"), ("scoreDesign", "4"), ("scoreSupport", "4"), ("feedback", "testOfFeedback"))
      val controller = new ApplicationController(messagesApi, env,
        app.injector.instanceOf[CustomGitHubProvider],
        app.injector.instanceOf[RepositoryService],
        app.injector.instanceOf[UserService],
        app.injector.instanceOf[GitHubAPI],
        app.injector.instanceOf[QuickstartService])

      val firstVisit = controller.gitHubRepository("elastic", "elasticsearch", None).apply(request)
      status(firstVisit) must equalTo(200)

      val result = controller.giveScorePage("elastic", "elasticsearch").apply(request)
      status(result) must equalTo(303)

    }

  }

  "User Logged Off " should {
    "not be able to see the button to submit feedback to repo" in new WithApplication {
      val request = FakeRequest()

      val controller = app.injector.instanceOf[ApplicationController]

      val result = controller.gitHubRepository("elastic", "elasticsearch", None).apply(request)
      status(result) must equalTo(OK)

      val body = new String(contentAsBytes(result))
      body must contain("elasticsearch")
      body must not contain ("href=\"/github/elastic/elasticsearch/feedback\" class=\"ui primary button\"")
    }
  }

  "see the login page when trying to submit feedback" in new WithApplication {
    val request = FakeRequest().withSession("csrfToken" -> CSRF.SignedTokenProvider.generateToken)

    val controller = app.injector.instanceOf[ApplicationController]

    val result = controller.giveFeedbackPage("elastic", "elasticsearch").apply(request)
    status(result) must equalTo(OK)

    val body = new String(contentAsBytes(result))
    body must contain("elasticsearch")
    body must contain("You need to be login to add some feedback")
  }


}

