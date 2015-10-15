package integration

import com.google.inject.Guice
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.authenticators.{SessionAuthenticator, CookieAuthenticator}
import com.mohiva.play.silhouette.test.{FakeEnvironment, _}
import controllers.ApplicationController
import models.User
import models.daos.drivers.GitHubAPI
import models.services.{QuickstartService, RepositoryService, UserService}
import modules.CustomGitHubProvider
import org.specs2.matcher._
import org.specs2.mock.mockito.MockitoStubs
import org.specs2.specification._
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test._
import setup.TestSetup
import scala.concurrent.ExecutionContext.Implicits.global


class UserBasedSpec extends PlaySpecification  {


  "The main Controller" should {
    "should find the proper repo" in new WithApplication {
      val identity = User(LoginInfo("github", "test@test.com"),
        "user",
        Option("userFullName"),
        Option("test@test.com"),
        None,
        1,
        None, // An ETag is used to know if the data has been modified since the last poll
        None
      ) // Used to filter the already retrieved events)
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

      val result = controller.gitHubRepository("elastic","elasticsearch",None).apply(request)
      status(result) must equalTo(OK)

      val body = new String(contentAsBytes(result))
      body must contain("elasticsearch")

    }
  }
}