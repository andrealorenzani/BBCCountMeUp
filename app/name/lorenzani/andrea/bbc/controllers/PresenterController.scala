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

package name.lorenzani.andrea.bbc.controllers

import jakarta.inject.{Inject, Singleton}
import name.lorenzani.andrea.bbc.model.JsonFormats.given
import name.lorenzani.andrea.bbc.model.VotingError
import name.lorenzani.andrea.bbc.repository.VotingRepository
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.mvc.*

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

@Singleton
class PresenterController @Inject() (val controllerComponents: ControllerComponents, repository: VotingRepository)(using
    ExecutionContext
) extends BaseController
    with ApiSupport:

  import PresenterController.*

  def index: Action[AnyContent] = Action(Ok(views.html.presenter()))

  def event: Action[AnyContent] = Action.async {
    repository.currentEvent().map(event => Ok(Json.toJson(event))).recover(handleVotingErrors)
  }

  def result: Action[AnyContent] = Action.async {
    repository.results().map(results => Ok(Json.toJson(results))).recover(handleVotingErrors)
  }

  /** Pushes the event and its totals every few seconds; whatever the client sends is ignored. */
  def websocket: WebSocket = WebSocket.accept[JsValue, JsValue] { _ =>
    val snapshots: Source[JsValue, NotUsed] =
      Source
        .tick(InitialDelay, RefreshInterval, ())
        .mapAsync(parallelism = 1)(_ => repository.snapshot().map(Json.toJson(_)).recover(noEvent))
        .mapMaterializedValue(_ => NotUsed)
    Flow.fromSinkAndSource(Sink.ignore, snapshots)
  }

object PresenterController:
  val InitialDelay: FiniteDuration = 1.second
  val RefreshInterval: FiniteDuration = 5.seconds

  /** Before the first event is opened there is nothing to show, which is not an error for a live view. */
  private val noEvent: PartialFunction[Throwable, JsValue] =
    case VotingError.NoOpenEvent => Json.obj("event" -> JsNull, "results" -> Json.arr())
