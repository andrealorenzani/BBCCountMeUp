package name.lorenzani.andrea.bbc.database

import javax.inject.Inject

import name.lorenzani.andrea.bbc.database.DbAccess.{Event, Votes}
import play.api.db.Database

// https://www.playframework.com/documentation/2.5.x/ScalaDependencyInjection
// or also https://github.com/google/guice/wiki/Injections
class PresenterDB @Inject() (database: Database) extends DbAccess{
  override val db: Database = database

  override def getCurrentEvent: Event = super.getCurrentEvent

  override def getVoteResult(eventId: Option[Int] = None): List[Votes] = super.getVoteResult(eventId)
}