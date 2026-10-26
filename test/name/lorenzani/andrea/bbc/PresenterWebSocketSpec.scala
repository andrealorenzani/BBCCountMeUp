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

import java.net.URI
import java.net.http.{HttpClient, WebSocket}
import java.util.concurrent.{CompletableFuture, CompletionStage, LinkedBlockingQueue, TimeUnit}

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import play.api.libs.ws.WSClient
import play.api.libs.ws.JsonBodyWritables.*
import play.api.http.Status.CREATED
import play.api.test.Helpers

import scala.concurrent.Await
import scala.concurrent.duration.*

/** End-to-end: a real server, a real HTTP client and a real WebSocket client. */
class PresenterWebSocketSpec extends PlaySpec with GuiceOneServerPerSuite:

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(Helpers.inMemoryDatabase()).build()

  private def connect(): LinkedBlockingQueue[JsValue] =
    val messages = LinkedBlockingQueue[JsValue]()
    val listener = new WebSocket.Listener:
      override def onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage[?] =
        messages.add(Json.parse(data.toString))
        webSocket.request(1)
        CompletableFuture.completedFuture(null)
      override def onOpen(webSocket: WebSocket): Unit = webSocket.request(1)
    HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create(s"ws://localhost:$port/presenter/websocket"), listener).get(10, TimeUnit.SECONDS)
    messages

  "The presenter websocket" must {

    "push an empty snapshot, then the live results once an event exists" in {
      val messages = connect()
      val first = messages.poll(15, TimeUnit.SECONDS)
      (first \ "event").get mustBe JsNull
      (first \ "results").as[JsArray].value mustBe empty

      val ws = app.injector.instanceOf[WSClient]
      val body = Json.obj("name" -> "Live", "description" -> "d", "candidates" -> Json.arr(Json.obj("name" -> "A", "initialVotes" -> 5), Json.obj("name" -> "B")))
      Await.result(ws.url(s"http://localhost:$port/admin/event").post(body), 10.seconds).status mustBe CREATED

      // keep reading until the push that follows the creation arrives
      val deadline = System.nanoTime() + 20.seconds.toNanos
      var snapshot = messages.poll(10, TimeUnit.SECONDS)
      while (snapshot \ "event" \ "name").asOpt[String].isEmpty && System.nanoTime() < deadline do
        snapshot = messages.poll(10, TimeUnit.SECONDS)
      (snapshot \ "event" \ "name").as[String] mustBe "Live"
      (snapshot \ "results").as[JsArray] mustBe Json.parse("""[{"name":"A","votes":5},{"name":"B","votes":0}]""")
    }
  }
