package name.lorenzani.andrea.bbc.actors

import javax.inject.Inject

import akka.actor.Actor
import name.lorenzani.andrea.bbc.database.VoterDB
import play.api.db.Database

class VoterActor @Inject() (db: Database) extends Actor {
  import name.lorenzani.andrea.bbc.actors.VoterActor._

  private lazy val voterDB = new VoterDB(db)

  override def receive: Receive = {
    case CurrentEvent() =>
      sender() ! voterDB.getCurrentEvent
    case InsertVote(voter, candidateId) =>
      voterDB.insertVote(voter, candidateId)
    case _ =>
      sender() ! "Not implemented"
  }
}

object VoterActor {
  case class CurrentEvent()
  case class InsertVote(voter: String, candidateId: Int)
}
