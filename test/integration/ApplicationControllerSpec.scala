package integration

import org.specs2.matcher.XmlMatchers
import org.specs2.specification.BeforeAfterEach
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}
import setup.TestSetup


class ApplicationControllerSpec extends PlaySpecification with BeforeAfterEach with XmlMatchers {

  def before = TestSetup.populateNeo4JData()

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

  def after = TestSetup.clearNeo4JData
}
