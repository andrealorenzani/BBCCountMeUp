package name.lorenzani.andrea.bbc.controllers

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Named}

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import name.lorenzani.andrea.bbc.database.DbAccess.{Event, Votes}
import name.lorenzani.andrea.bbc.Utils.JsonWrites._
import play.api.Logger
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, AnyContent, Controller}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, FiniteDuration}

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

  def addVote: Action[AnyContent] = Action.async { request =>
    import name.lorenzani.andrea.bbc.actors.VoterActor._
    import name.lorenzani.andrea.bbc.Utils.JsonReads.voteFromVoter
    implicit val timeout = Timeout(FiniteDuration(10, TimeUnit.SECONDS))
    Future {
      request.body.asJson.map { json =>
        json.validate[(String, Int, Int)].map {
          case (voter, candidateId, eventId) =>
            voterActor ! InsertVote(voter, candidateId)
            Logger.info(s"New vote added by $voter: $candidateId")
            Ok(Json.toJson(Map("msg"->"Vote added")))
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
    Ok(views.html.voter())
  }
}
