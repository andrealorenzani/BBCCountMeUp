import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.Mode
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends Specification {

  lazy val app = new GuiceApplicationBuilder().configure(Helpers.inMemoryDatabase()).in(Mode.Test).build()

  "Application" should {

    "work from within a browser" in new WithBrowser(app = app) {

      browser.goTo("http://localhost:" + port)

      browser.pageSource must contain("Your new application is ready.")
    }
  }
}
