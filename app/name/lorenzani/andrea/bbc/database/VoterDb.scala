package name.lorenzani.andrea.bbc.database

import javax.inject.Inject

import name.lorenzani.andrea.bbc.database.DbAccess.{Event, Votes}
import play.api.db.Database

// https://www.playframework.com/documentation/2.5.x/ScalaDependencyInjection
// or also https://github.com/google/guice/wiki/Injections
class VoterDB @Inject() (database: Database) extends DbAccess{
  override val db: Database = database

  override def getCurrentEvent: Event = super.getCurrentEvent

  override def insertVote(voter: String, candidate: Int): Unit = super.insertVote(voter, candidate)
}
