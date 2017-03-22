package name.lorenzani.andrea.bbc.actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.pattern.ask
import akka.util.Timeout
import name.lorenzani.andrea.bbc.actors.PresenterActor.VoteResult
import name.lorenzani.andrea.bbc.database.DbAccess.Votes
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.duration.FiniteDuration

object WebSocketActor {
  def props(out: ActorRef, dbActor: ActorRef) = Props(new WebSocketActor(out, dbActor))

  case class Refresh()
}

class WebSocketActor(out: ActorRef, dbActor: ActorRef) extends Actor {
  import name.lorenzani.andrea.bbc.actors.WebSocketActor._

  var cancellable: Option[Cancellable] = None

  override def preStart(): Unit = {
    import scala.concurrent.duration._
    super.preStart()
    cancellable = Some(context.system.scheduler.schedule(2.seconds, 5.seconds, self, Refresh()))
  }

  override def postStop(): Unit = {
    super.postStop()
    cancellable.map(x => x.cancel())
  }

  def receive = {
    case Refresh() =>
      import name.lorenzani.andrea.bbc.Utils.JsonWrites._
      implicit val timeout = Timeout(FiniteDuration(10, TimeUnit.SECONDS))
      (dbActor ? VoteResult()).map(votes=> out ! Json.toJson(votes.asInstanceOf[List[Votes]]))
    case msg: String =>
      out ! Json.toJson(Map("msg" -> s"I received your message: $msg"))
  }
}