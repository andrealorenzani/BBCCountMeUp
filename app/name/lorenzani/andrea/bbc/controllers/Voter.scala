package name.lorenzani.andrea.bbc.controllers

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.util.Timeout
import name.lorenzani.andrea.bbc.database.DbAccess.Event
import name.lorenzani.andrea.bbc.database.VoterDB
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Controller}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import name.lorenzani.andrea.bbc.Utils.JsonWrites._
import name.lorenzani.andrea.bbc.Utils.JsonReads._

class Voter @Inject()(voterDB: VoterDB) extends Controller {
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def event: Action[AnyContent] = Action.async {
    implicit val timeout = Timeout(FiniteDuration(10, TimeUnit.SECONDS))

    //https://www.playframework.com/documentation/2.5.x/ScalaAsync
    Future[Event] {
      voterDB.getCurrentEvent
    }.map { event =>
      // https://www.playframework.com/documentation/2.1.0/ScalaJsonRequests
      Ok(Json.toJson(event))
    }.recover {
      case error => BadRequest(Json.toJson(Map("type" -> "error", "message" -> error.getMessage)))
    }
  }

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
}
