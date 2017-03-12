package name.lorenzani.andrea.bbc.controllers

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.util.Timeout
import name.lorenzani.andrea.bbc.database.AdminDB
import name.lorenzani.andrea.bbc.database.DbAccess.{Event, Votes}
import play.api.Logger
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, AnyContent, Controller}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, Future}
import name.lorenzani.andrea.bbc.Utils.JsonWrites._
import name.lorenzani.andrea.bbc.Utils.JsonReads._

class Admin @Inject()(adminDB: AdminDB) extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def event: Action[AnyContent] = Action.async {
    implicit val timeout = Timeout(FiniteDuration(10, TimeUnit.SECONDS))

    //https://www.playframework.com/documentation/2.5.x/ScalaAsync
    Future[Event] {
      adminDB.getCurrentEvent
    }.map { event =>
      // https://www.playframework.com/documentation/2.1.0/ScalaJsonRequests
      Ok(Json.toJson(event))
    }.recover {
      case error => BadRequest(Json.toJson(Map("type" -> "error", "message" -> error.getMessage)))
    }
  }

  def addEvent: Action[AnyContent] = Action.async { request =>
    Future {
      request.body.asText.foreach(Logger.info(_))
      request.body.asJson.map { json =>
        json.validate[(Event, List[Votes])].map {
          case (event, votes) =>
            // not in parallel, we want to be sure it is created before adding candidates
            adminDB.openEvent(event.name, event.desc, true)
            Logger.info(s"Opened new event ${event.name}: '${event.desc}'")
            val parallelAdd = votes.map { cand => Future{ adminDB.insertCandidate(cand.name, cand.nvotes.toInt) } }
            // We don't really care about the result, this is a massive add done at event creation time, and
            // admin has no need to wait for the result, as long as the data is inserted, at one point
            val nvotes = votes.foreach(vote => Logger.info(s"Adding candidate ${vote.name} (with initial number of votes ${vote.nvotes})"))
            Await.result(Future.sequence(parallelAdd), Duration.Inf)
            Ok(Json.toJson(event))
        }.recoverTotal {
          e => BadRequest(JsError.toFlatJson(e))
        }
      }.getOrElse {
        val txt = request.body.asText.getOrElse("<empty_string />")
        BadRequest(s"Expecting Json data and not '$txt'")
      }
    }
  }


  def index = Action {
    Ok(views.html.admin("Your new application is ready."))
  }
}
