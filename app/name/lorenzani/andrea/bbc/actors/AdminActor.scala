package name.lorenzani.andrea.bbc.actors

import javax.inject.Inject

import akka.actor.Actor
import name.lorenzani.andrea.bbc.database.AdminDB
import play.api.db.Database

class AdminActor @Inject()(db: Database) extends Actor {
  import name.lorenzani.andrea.bbc.actors.AdminActor._

  private lazy val adminDB = new AdminDB(db)

  override def receive: Receive = {
    case CurrentEvent() =>
      sender() ! adminDB.getCurrentEvent
    case OpenEvent(evName, evDesc, isCurr) =>
      adminDB.openEvent(evName, evDesc, isCurr)
      sender() ! true
    case InsertCandidate(candidate, votes, evId) =>
      adminDB.insertCandidate(candidate, votes, evId)
      sender() ! true
    case _ =>
      sender() ! "Not implemented"
  }
}

object AdminActor {
  case class CurrentEvent()
  case class OpenEvent(eventName: String, eventDesc: String, isCurrent: Boolean)
  case class InsertCandidate(candidateName: String, initialVotes: Int, eventId: Option[Int] = None)
}
