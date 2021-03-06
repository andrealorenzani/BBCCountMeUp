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

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import name.lorenzani.andrea.bbc.database.DbAccess.{Event, Votes}
import name.lorenzani.andrea.bbc.Utils.JsonWrites._
import name.lorenzani.andrea.bbc.actors.WebSocketActor
import play.api.libs.json._
import play.api.libs.streams.ActorFlow
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc.{Action, AnyContent, Controller, WebSocket}

import scala.concurrent.duration.FiniteDuration

class Presenter @Inject()(implicit @Named("presenter-actor") presenterActor: ActorRef,
                          system: ActorSystem,
                          materializer: Materializer) extends Controller {
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def websocket = {
    implicit val messageFlowTransformer = MessageFlowTransformer.jsonMessageFlowTransformer[String, JsValue]
    WebSocket.accept[String, JsValue] { request =>
      ActorFlow.actorRef(out => WebSocketActor.props(out, presenterActor))
    }
  }

  def result: Action[AnyContent] = Action.async {
    import name.lorenzani.andrea.bbc.actors.PresenterActor.VoteResult
    implicit val timeout = Timeout(FiniteDuration(10, TimeUnit.SECONDS))

    // https://www.playframework.com/documentation/2.5.x/ScalaAsync
    (presenterActor ? VoteResult()).map { response =>
      // https://www.playframework.com/documentation/2.1.0/ScalaJsonRequests
      Ok(Json.toJson(response.asInstanceOf[List[Votes]]))
    }.recover {
      case error => BadRequest(Json.toJson(Map("type" -> "error", "message" -> error.getMessage)))
    }
  }

  def event: Action[AnyContent] = Action.async {
    import name.lorenzani.andrea.bbc.actors.PresenterActor.CurrentEvent
    implicit val timeout = Timeout(FiniteDuration(10, TimeUnit.SECONDS))

    // https://www.playframework.com/documentation/2.5.x/ScalaAsync
    (presenterActor ? CurrentEvent()).map { response =>
      // https://www.playframework.com/documentation/2.1.0/ScalaJsonRequests
      Ok(Json.toJson(response.asInstanceOf[Event]))
    }.recover {
      case error => BadRequest(Json.toJson(Map("type" -> "error", "message" -> error.getMessage)))
    }
  }

  def index = Action {
    Ok(views.html.presenter("Your new application is ready."))
  }
}
