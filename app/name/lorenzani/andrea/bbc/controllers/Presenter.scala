package name.lorenzani.andrea.bbc.controllers

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.util.Timeout
import name.lorenzani.andrea.bbc.database.DbAccess.Votes
import name.lorenzani.andrea.bbc.database.PresenterDB
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Controller}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import name.lorenzani.andrea.bbc.Utils.JsonWrites._

class Presenter @Inject()(presenterDB: PresenterDB) extends Controller {
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def result: Action[AnyContent] = Action.async {
    implicit val timeout = Timeout(FiniteDuration(10, TimeUnit.SECONDS))

    // https://www.playframework.com/documentation/2.5.x/ScalaAsync
    Future[List[Votes]] {
      presenterDB.getVoteResult()
    }.map { response =>
      // https://www.playframework.com/documentation/2.1.0/ScalaJsonRequests
      Ok(Json.toJson(response))
    }.recover {
      case error => BadRequest(Json.toJson(Map("type" -> "error", "message" -> error.getMessage)))
    }
  }

  def index = Action {
    Ok(views.html.presenter("Your new application is ready."))
  }
}
