/***
*   Copyright 2017 Andrea Lorenzani
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
***/

package name.lorenzani.andrea.bbc

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import play.api.mvc.Result
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}

import scala.concurrent.Future

/** HTTP-level tests: routing, JSON contracts, status codes and the rendered pages. */
class ApiSpec extends PlaySpec with GuiceOneAppPerSuite:

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(Helpers.inMemoryDatabase()).build()

  private def get(path: String): Future[Result] = route(app, FakeRequest(GET, path)).get

  private def post(path: String, body: JsValue): Future[Result] =
    route(app, FakeRequest(POST, path).withJsonBody(body)).get

  private def eventJson(name: String = "Best song", candidates: Seq[JsObject] = Seq(Json.obj("name" -> "A", "initialVotes" -> 2), Json.obj("name" -> "B"))) =
    Json.obj("name" -> name, "description" -> "Vote now", "candidates" -> candidates)

  private def vote(voter: String, candidateId: Int, eventId: Int) =
    post("/voter/vote", Json.obj("voter" -> voter, "candidateId" -> candidateId, "eventId" -> eventId))

  "Before any event is opened" must {
    "answer 404 with a JSON error from every read endpoint" in {
      // first test of the suite on purpose: later tests open events
      Seq("/admin/event", "/voter/event", "/presenter/event", "/presenter/result").foreach { path =>
        val response = get(path)
        status(response) mustBe NOT_FOUND
        (contentAsJson(response) \ "error").as[String] mustBe "No event is open yet"
      }
    }
  }

  "The pages" must {
    "redirect the root to the presenter" in {
      val response = get("/")
      status(response) mustBe SEE_OTHER
      redirectLocation(response) mustBe Some("/presenter")
    }

    "render presenter, voter and admin as HTML with their own script" in {
      Seq("/presenter" -> "presenter.js", "/voter" -> "voter.js", "/admin" -> "admin.js").foreach { (path, script) =>
        val response = get(path)
        status(response) mustBe OK
        contentType(response) mustBe Some("text/html")
        contentAsString(response) must include(script)
        contentAsString(response) must include("CountMeUp")
      }
    }

    "serve the vendored libraries and scripts" in {
      Seq("vendor/chart.umd.min.js", "vendor/bootstrap.min.css", "javascripts/api.js", "stylesheets/main.css").foreach { asset =>
        status(get(s"/assets/$asset")) mustBe OK
      }
    }

    "send a strict content security policy" in {
      val policy = header("Content-Security-Policy", get("/presenter")).value
      policy must include("default-src 'self'")
      policy must include("script-src 'self'")
      policy must not include "unsafe-inline"
    }

    "answer 404 for unknown paths" in {
      status(get("/boum")) mustBe NOT_FOUND
    }
  }

  "The admin API" must {
    "reject an invalid event listing every problem" in {
      val response = post("/admin/event", eventJson(name = " ", candidates = Seq(Json.obj("name" -> "A"))))
      status(response) mustBe BAD_REQUEST
      (contentAsJson(response) \ "errors").as[Seq[String]] must contain allOf ("The event needs a name", "At least 2 candidates are required")
    }

    "reject a body that is not the expected JSON" in {
      status(post("/admin/event", Json.obj("name" -> "only a name"))) mustBe BAD_REQUEST
      status(route(app, FakeRequest(POST, "/admin/event").withTextBody("hello")).get) mustBe UNSUPPORTED_MEDIA_TYPE
    }

    "create an event and make it the current one" in {
      val created = post("/admin/event", eventJson())
      status(created) mustBe CREATED
      val event = contentAsJson(created)
      (event \ "name").as[String] mustBe "Best song"
      (event \ "candidates" \\ "name").map(_.as[String]) mustBe Seq("A", "B")

      contentAsJson(get("/admin/event")) mustBe event
      contentAsJson(get("/voter/event")) mustBe event
      contentAsJson(get("/presenter/event")) mustBe event
      contentAsJson(get("/presenter/result")) mustBe Json.parse("""[{"name":"A","votes":2},{"name":"B","votes":0}]""")
    }
  }

  "The voter API" must {
    "count votes, enforce the limit and refuse stale or unknown input" in {
      val event = contentAsJson(post("/admin/event", eventJson("Voting", Seq(Json.obj("name" -> "A"), Json.obj("name" -> "B"))))).as[JsObject]
      val eventId = (event \ "id").as[Int]
      val Seq(a, b) = (event \ "candidates" \\ "id").map(_.as[Int]).toSeq

      status(vote("alice", a, eventId)) mustBe CREATED
      status(vote("alice", a, eventId)) mustBe CREATED
      status(vote("alice", b, eventId)) mustBe CREATED
      val limited = vote("alice", b, eventId)
      status(limited) mustBe CONFLICT
      (contentAsJson(limited) \ "error").as[String] must include("maximum number of votes")

      status(vote("bob", 99999, eventId)) mustBe NOT_FOUND
      status(vote("bob", a, eventId + 100)) mustBe CONFLICT
      status(vote("   ", a, eventId)) mustBe BAD_REQUEST
      status(post("/voter/vote", Json.obj("voter" -> "bob"))) mustBe BAD_REQUEST

      contentAsJson(get("/presenter/result")) mustBe Json.parse("""[{"name":"A","votes":2},{"name":"B","votes":1}]""")
    }
  }
