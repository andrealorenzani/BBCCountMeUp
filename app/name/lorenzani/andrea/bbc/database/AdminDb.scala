package name.lorenzani.andrea.bbc.database

import javax.inject.Inject
import name.lorenzani.andrea.bbc.database.DbAccess.Event
import play.api.db.Database

// https://www.playframework.com/documentation/2.5.x/ScalaDependencyInjection
// or also https://github.com/google/guice/wiki/Injections
class AdminDB @Inject() (database: Database) extends DbAccess {
  override val db: Database = database

  override def insertCandidate(name: String, initialVotes: Int = 0, eventId: Option[Int] = None): Unit = super.insertCandidate(name, initialVotes, eventId)

  override def openEvent(name: String, description: String, current: Boolean): Unit = super.openEvent(name, description, current)

  override def getCurrentEvent: Event = super.getCurrentEvent
}