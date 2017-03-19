package name.lorenzani.andrea.bbc.actors

import javax.inject.Inject

import akka.actor.Actor
import name.lorenzani.andrea.bbc.database.PresenterDB
import play.api.db.Database

class PresenterActor @Inject()(db: Database) extends Actor {
  import name.lorenzani.andrea.bbc.actors.PresenterActor._

  private lazy val presenterDB = new PresenterDB(db)

  override def receive: Receive = {
    case CurrentEvent() =>
      sender() ! presenterDB.getCurrentEvent
    case VoteResult(eventId) =>
      sender() ! presenterDB.getVoteResult(eventId)
    case _ =>
      sender() ! "Not implemented"
  }
}

object PresenterActor {
  case class CurrentEvent()
  case class VoteResult(eventId: Option[Int] = None)
}
