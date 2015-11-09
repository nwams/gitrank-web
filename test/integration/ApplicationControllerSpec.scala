package integration

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import com.mohiva.play.silhouette.test._
import controllers.ApplicationController
import models.User
import models.daos.drivers.GitHubAPI
import models.services.{QuickstartService, RepositoryService, UserService}
import modules.CustomGitHubProvider
import org.specs2.matcher.XmlMatchers
import org.specs2.specification.BeforeAfterEach
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsArray, JsObject}
import play.api.test._
import play.filters.csrf.CSRF
import setup.{TestSetup, WithGitHub}

import scala.concurrent.ExecutionContext.Implicits.global


class ApplicationControllerSpec extends PlaySpecification with BeforeAfterEach with XmlMatchers with WithGitHub {

  def before = TestSetup.populateNeo4JData()

  def createAuthApplicationController(app: Application, user: User) = {
    val messagesApi = app.injector.instanceOf[MessagesApi]
    val gitHubProvider = app.injector.instanceOf[CustomGitHubProvider]
    val repoService = app.injector.instanceOf[RepositoryService]
    val userService = app.injector.instanceOf[UserService]
    val gitHub = app.injector.instanceOf[GitHubAPI]
    val quickstartService = app.injector.instanceOf[QuickstartService]

    implicit val env = FakeEnvironment[User, SessionAuthenticator](Seq(
      user.loginInfo -> user
    ))

    val request = FakeRequest()
      .withSession("csrfToken" -> CSRF.SignedTokenProvider.generateToken)
      .withAuthenticator(user.loginInfo)

    val controller = new ApplicationController(messagesApi, env, gitHubProvider, repoService, userService, gitHub, quickstartService)

    (request, controller)
  }

  "Home page" should {
    "be delivered correctly" in new WithApplication {
      val page = route(FakeRequest(GET, "/")).get

      status(page) must equalTo(OK)
      contentType(page) must beSome.which(_ == "text/html")
    }

    "should contain a list of most starred repo when user is not connected" in new WithApplication {
      val content = contentAsString(route(FakeRequest(GET, "/")).get)

      content must contain("twbs/bootstrap")
      content must contain("vhf/free-programming-books")
      content must contain("angular/angular.js")
      content must contain("mbostock/d3")
      content must contain("nodejs/node-v0.x-archive")
      content must contain("jquery/jquery")
    }

    "should contain a list of the user's starred repo when he is connected" in new WithApplication {

      val mocked = createAuthApplicationController(app,
        User(LoginInfo("github", "4429209"), "callicles" ,Some("Nicolas Newman"), Some("josh@newman.com"),
          Some("http://api.adorable.io/avatars/285/josh@newman.com.png"), 300, None, None)
      )

      val page = mocked._2.index()(mocked._1)
      status(page) must equalTo(OK)
      contentType(page) must beSome.which(_ == "text/html")

      val content = contentAsString(page)
      content must contain("tyrcho/openshift-cartridge-play2")
      content must contain("jroper/play-iteratees-extras")
      content must contain("fivethirtyeight/uber-tlc-foil-response")
      content must contain("Cornell-CS5356-Fall2015/cs5356")
      content must contain("cornelltech/startup-ideas")
      content must contain("cornelltech/company-challenges")
    }
  }

  "Repository page" should {
    "be delivered correctly" in new WithApplication {
      val page = route(FakeRequest(GET, "/github/test/test1")).get

      status(page) must equalTo(OK)
      contentType(page) must beSome.which(_ == "text/html")
    }

    "contain feedback information" in new WithApplication {
      val content = contentAsString(route(FakeRequest(GET, "/github/test/test1")).get)
      content must contain("This is an outstanding repo")
      content must contain("<span data-content=\"Documentation\" class=\"popup\"> 4 <i class=\"icon book\"></i></span>")
      content must contain("<span data-content=\"Maturity\" class=\"popup\"> 1 <i class=\"icon leaf\"></i></span>")
      content must contain("<span data-content=\"Design\" class=\"popup\"> 3 <i class=\"icon university\"></i></span>")
      content must contain("<span data-content=\"Support\" class=\"popup\"> 2 <i class=\"icon life ring\"></i></span>")
    }

    "contain score" in new WithApplication {
      val content = contentAsString(route(FakeRequest(GET, "/github/test/test1")).get)
      content must contain("1.5/5")
    }
  }

  "Quickstart post endpoint" should {
    "prevent posting if the user is not connected" in new WithApplication {
      val response = route(FakeRequest(POST, "/github/test/test2/quickstart")).get
      // TODO This should be changed to 401
      status(response) must equalTo(303)
    }

    "be able to post a quickstart if the user is connected" in new WithApplication {

      val mocked = createAuthApplicationController(app,
        User(LoginInfo("github", "User1"), "User1" ,Some("Josh Newman"), Some("josh@newman.com"),
          Some("http://api.adorable.io/avatars/285/josh@newman.com.png"), 180, None, None)
      )

      val request = mocked._1
        .withFormUrlEncodedBody(
          ("title", "Added Quickstart"),
          ("url", "http://test.com"),
          ("description", "a super test guide")
        )

      val result = mocked._2.postQuickstartGuide("test", "test2")(request)

      // TODO This should be changed to 200
      status(result) must equalTo(303)

      val json = route(FakeRequest(GET, "/github/test/test2/quickstart/guides")).get
      status(json) must equalTo(OK)
      val content = contentAsJson(json).as[JsArray].value.head
      (content \ "title").as[String] must equalTo("Added Quickstart")
      (content \ "description").as[String] must equalTo("a super test guide")
      (content \ "url").as[String] must equalTo("http://test.com")
      (content \ "upvote").as[Int] must equalTo(0)
      (content \ "downvote").as[Int] must equalTo(0)
    }
  }

  "give feedback post endpoint" should {
    "prevent posting if the user is not connected" in new WithApplication {
      val response = route(FakeRequest(POST, "/github/test/test2/feedback")).get
      // TODO this should be changed to 401
      status(response) must equalTo(303)
    }

    "be able to post feedback if the user is connected" in new WithApplication {

      val mocked = createAuthApplicationController(app,
        User(LoginInfo("github", "User1"), "User1" ,Some("Josh Newman"), Some("josh@newman.com"),
          Some("http://api.adorable.io/avatars/285/josh@newman.com.png"), 180, None, None)
      )

      val request = mocked._1
        .withFormUrlEncodedBody(
          ("scoreDocumentation", "3"),
          ("scoreDesign", "3"),
          ("scoreMaturity", "1"),
          ("scoreSupport", "1"),
          ("feedback", "Test feedback")
        )

      val result = mocked._2.postScore("test", "test2", None)(request)

      // TODO This should be changed to 200
      status(result) must equalTo(303)

      val page = route(FakeRequest(GET, "/github/test/test2")).get
      status(page) must equalTo(OK)
      val content = contentAsString(page)
      content must contain("Test feedback")
      // Computed score
      content must contain("2.0/5")
      // Detail of the score
      content must contain("3")
    }
  }

  "up vote post endpoint" should {
    "prevent posting if the user is not connected" in new WithApplication {
      val json = route(FakeRequest(GET, "/github/test/test1/quickstart/guides")).get
      val quickstart = contentAsJson(json).as[JsArray].value.head

      val response = route(FakeRequest(POST, "/github/test/test2/quickstart/" + (quickstart \ "id").as[Int] +"/upvote")).get
      // TODO this should be changed to 401
      status(response) must equalTo(303)
    }

    "should not be able to post an up vote if the user is has already voted" in new WithApplication {

      val mocked = createAuthApplicationController(app,
        User(LoginInfo("github", "User1"), "User1" ,Some("Josh Newman"), Some("josh@newman.com"),
          Some("http://api.adorable.io/avatars/285/josh@newman.com.png"), 180, None, None)
      )

      val json = route(FakeRequest(GET, "/github/test/test1/quickstart/guides")).get
      val quickstart = contentAsJson(json).as[JsArray].value.head
      val result = mocked._2.upVote("test", "test1", (quickstart \ "id").as[Int], "upvote")(mocked._1)

      status(result) must equalTo(OK)

      val content = contentAsJson(result).as[JsObject]
      (content \ "title").as[String] must equalTo("Top 10 of the test quickstart")
      (content \ "description").as[String] must equalTo("A comprehensive overview of all the testing techniques")
      (content \ "url").as[String] must equalTo("http://www.nikelodeon.com")
      (content \ "upvote").as[Int] must equalTo(3)
      (content \ "downvote").as[Int] must equalTo(1)
    }

    "should be able to post an up vote if the user did not already voted" in new WithApplication {


      val mocked = createAuthApplicationController(app,
        User(LoginInfo("github", "User2"), "User2" ,Some("Samuel Adams"), Some("sam@adams.com"),
          Some("http://api.adorable.io/avatars/285/josh@newman.com.png"), 180, None, None)
      )

      val json = route(FakeRequest(GET, "/github/test/test1/quickstart/guides")).get
      val quickstart = contentAsJson(json).as[JsArray].value.head
      val result = mocked._2.upVote("test", "test1", (quickstart \ "id").as[Int], "upvote")(mocked._1)

      status(result) must equalTo(OK)

      val content = contentAsJson(result).as[JsObject]
      (content \ "title").as[String] must equalTo("Top 10 of the test quickstart")
      (content \ "description").as[String] must equalTo("A comprehensive overview of all the testing techniques")
      (content \ "url").as[String] must equalTo("http://www.nikelodeon.com")
      (content \ "upvote").as[Int] must equalTo(4)
      (content \ "downvote").as[Int] must equalTo(1)
    }
  }

  def after = TestSetup.clearNeo4JData
}
