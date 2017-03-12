package name.lorenzani.andrea.bbc.database

import play.api.db.Database

object DbAccess {
  case class Votes(name: String, nvotes: Long)
  case class Candidate(id: Int, name: String)
  case class Event(id: Int, name: String, desc: String, candidates: List[Candidate])
}

// For simplicity I define here all the queries I need. This is a trait, I think I will need
// 3 actors for the access: one that is able to retrieve the result of the vote, one for
// the admin part and one for the voter part.
// Ref for db in Play: https://www.playframework.com/documentation/2.5.x/ScalaDatabase
trait DbAccess {
  import DbAccess._

  val db: Database


  protected def insertVote(voter: String, candidate: Int): Unit = ???

  protected def getVoteResult(eventId: Option[Int] = None): List[Votes] = ???

  protected def openEvent(name: String, description: String, current: Boolean): Unit = ???

  protected def getCurrentEvent: Event = ???

  protected def insertCandidate(name: String, initialVotes: Int = 0, eventId: Option[Int] = None): Unit = ???
}
