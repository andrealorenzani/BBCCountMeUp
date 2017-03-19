package name.lorenzani.andrea.bbc.controllers

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Named}

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import name.lorenzani.andrea.bbc.database.DbAccess.{Event, Votes}
import name.lorenzani.andrea.bbc.Utils.JsonWrites._
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Controller}

import scala.concurrent.duration.FiniteDuration

class Presenter @Inject()(@Named("presenter-actor") presenterActor: ActorRef) extends Controller {
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

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
