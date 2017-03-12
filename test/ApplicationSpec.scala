import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.Mode
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  lazy val app = new GuiceApplicationBuilder().configure(Helpers.inMemoryDatabase()).in(Mode.Test).build()

  "Application" should {

    "send 404 on a bad request" in {
      route(app, FakeRequest(GET, "/boum")) must beSome.which (status(_) == NOT_FOUND)
    }

    "render the index page" in {
      val home = route(app, FakeRequest(GET, "/")).get

      // SEE_OTHER??? https://www.playframework.com/documentation/2.5.x/JavaActions#redirects-are-simple-results-too
      status(home) must equalTo(SEE_OTHER)
      //contentType(home) must beSome.which(_ == "text/html")
      //contentAsString(home) must contain ("Your new application is ready.")
    }
  }
}
