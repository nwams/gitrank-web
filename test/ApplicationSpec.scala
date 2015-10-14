import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    "send 404 on a bad request" in new WithApplication {
      route(FakeRequest(GET, "/Boom")).map {
        res => status(res) must equalTo(404)
      }
    }

    "render the main page" in new WithApplication {
      route(FakeRequest(GET, "/")).map{
        home =>{
          status(home) must equalTo(OK)
          contentAsString(home) must contain("GitRank")
          contentType(home) must beSome.which(_ == "text/html")
        }
      }

    }


  }
}
