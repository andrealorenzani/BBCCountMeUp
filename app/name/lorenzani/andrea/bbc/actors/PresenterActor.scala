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
