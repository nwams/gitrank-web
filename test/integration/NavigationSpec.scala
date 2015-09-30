package integration

import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test.Helpers._
import play.api.test._

import scala.util.Properties

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class NavigationSpec extends Specification {

  "User" should {

    "be redirected to github when trying to login" in new WithBrowser {
      browser.goTo("/")
      browser.$(".octicon-mark-github").click()
      browser.url.toString must  contain("github.com")
    }
    "be able to find a repo" in new WithBrowser {
      browser.goTo("/github/angular/angular")
      assert(browser.$("#content").first().isDisplayed)
    }
    "be able to see feedback page" in new WithBrowser {
      browser.goTo("/github/angular/angular/feedback")
      assert(browser.$("#submit").first().isEnabled)
      assert(browser.$("#feedback_field").first().isEnabled)
      assert(browser.$("#docScore").first().isEnabled)
      assert(browser.$("#matScore ").first().isEnabled)
      assert(browser.$("#desScore").first().isEnabled)
      assert(browser.$("#supScore").first().isEnabled)


    }
  }
}
