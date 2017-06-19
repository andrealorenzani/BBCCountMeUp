/***
*   Copyright 2017 Andrea Lorenzani
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
***/

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
