package integration

import org.specs2.specification._
import play.api.test._
import setup.TestSetup

class PublicAPISpec extends PlaySpecification with BeforeEach {

  def before = TestSetup.populateNeo4JData()

  "The score feedback badge api" should {
    "deliver a correct svg file" in new WithApplication {
      running(FakeApplication()) {
        val svg = route(FakeRequest(GET, "/api/badges/github/test/test.svg")).get

        status(svg) must equalTo(OK)
        contentType(svg) must beSome.which(_ == "image/svg+xml")
      }
    }
  }
}