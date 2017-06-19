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
