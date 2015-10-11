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

    "send 404 on a bad request" in new WithApplication{
      val result = route(FakeRequest(GET, "/Boom")).get
      status(result) must equalTo(404)
    }

    "render the main page" in new WithApplication{
      val home = route(FakeRequest(GET, "/")).get

      status(home) must equalTo(OK)
      contentAsString(home) must contain("GitRank")
      contentType(home) must beSome.which(_ == "text/html")
    }

    //"get list of guides" in new WithApplication{
    //  val guides = route(FakeRequest(GET, "/github/angular/angular/quickstart/guides")).get
    //  status(guides) must equalTo(OK)
    //}
  }
}
