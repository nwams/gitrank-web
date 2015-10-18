package integration

import org.specs2.specification._
import play.api.libs.json.JsArray
import play.api.test._
import setup.TestSetup

class PublicAPISpec extends PlaySpecification with BeforeAfterEach {

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

  "The give feedback badge" should {
    "be a correct svg file" in new WithApplication {
      val svg = route(FakeRequest(GET, "/assets/images/giveFeedbackBadge.svg")).get
      status(svg) must equalTo(OK)
      contentType(svg) must beSome.which(_ == "image/svg+xml")
    }
  }

  "The quickstart API" should {
    "give a json response" in new WithApplication {
      val json = route(FakeRequest(GET, "/github/test/test1/quickstart/guides")).get
      status(json) must equalTo(OK)
      contentType(json) must beSome.which(_ == "application/json")
    }

    "list correctly the available feedback" in new WithApplication {
      val json = route(FakeRequest(GET, "/github/test/test1/quickstart/guides")).get
      status(json) must equalTo(OK)
      val content = contentAsJson(json).as[JsArray].value.head
      (content \ "timestamp").as[Double] must equalTo(1444426459006.0)
      (content \ "title").as[String] must equalTo("Top 10 of the test quickstart")
      (content \ "description").as[String] must equalTo("A comprehensive overview of all the testing techniques")
      (content \ "url").as[String] must equalTo("http://www.nikelodeon.com")
      (content \ "upvote").as[Int] must equalTo(3)
      (content \ "downvote").as[Int] must equalTo(1)
    }
  }


  def after = TestSetup.clearNeo4JData
}
