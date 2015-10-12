package integration

import org.specs2.matcher._
import org.specs2.specification._
import play.api.test._
import setup.TestSetup

class PublicAPISpec extends PlaySpecification with BeforeAfterEach with XmlMatchers {

  def before = TestSetup.populateNeo4JData()

  "The score feedback badge api" should {
    "deliver a correct svg file" in new WithApplication {

      val svg = route(FakeRequest(GET, "/api/badges/github/test/test0.svg")).get

      status(svg) must equalTo(OK)
      contentType(svg) must beSome.which(_ == "image/svg+xml")

    }

    "be red if the score is below 1" in new WithApplication {
      val svg = route(FakeRequest(GET, "/api/badges/github/test/test0.svg")).get
      contentAsString(svg) must contain("#e05d44")
    }

    "be orange if the score is between 1 and 2" in new WithApplication {
      val svg = route(FakeRequest(GET, "/api/badges/github/test/test1.svg")).get
      contentAsString(svg) must contain("#fe7d37")
    }

    "be yellow if the score is between 2 and 3" in new WithApplication {
      val svg = route(FakeRequest(GET, "/api/badges/github/test/test2.svg")).get
      contentAsString(svg) must contain("#dfb317")
    }

    "be green if the score is between 3 and 4" in new WithApplication {
      val svg = route(FakeRequest(GET, "/api/badges/github/test/test3.svg")).get
      contentAsString(svg) must contain("#97CA00")
    }

    "be bright green if the score is greater than 4" in new WithApplication {
      val svg = route(FakeRequest(GET, "/api/badges/github/test/test4.svg")).get
      contentAsString(svg) must contain("#4c1")
    }

  }

  def after = TestSetup.clearNeo4JData
}