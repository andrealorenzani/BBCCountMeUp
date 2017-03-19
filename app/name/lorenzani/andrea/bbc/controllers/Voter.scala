package name.lorenzani.andrea.bbc.controllers

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Named}

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import name.lorenzani.andrea.bbc.database.DbAccess.Event
import name.lorenzani.andrea.bbc.Utils.JsonWrites._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Controller}

import scala.concurrent.duration.FiniteDuration

class Voter @Inject()(@Named("voter-actor") voterActor: ActorRef) extends Controller {
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def event: Action[AnyContent] = Action.async {
    import name.lorenzani.andrea.bbc.actors.VoterActor.CurrentEvent
    implicit val timeout = Timeout(FiniteDuration(10, TimeUnit.SECONDS))

    //https://www.playframework.com/documentation/2.5.x/ScalaAsync
    (voterActor ? CurrentEvent()).map { event =>
      // https://www.playframework.com/documentation/2.1.0/ScalaJsonRequests
      Ok(Json.toJson(event.asInstanceOf[Event]))
    }.recover {
      case error => BadRequest(Json.toJson(Map("type" -> "error", "message" -> error.getMessage)))
    }
  }

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
}
