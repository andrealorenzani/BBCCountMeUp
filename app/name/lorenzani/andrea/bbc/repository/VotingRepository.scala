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

package name.lorenzani.andrea.bbc.repository

import com.google.inject.ImplementedBy
import name.lorenzani.andrea.bbc.model.*

import scala.concurrent.Future

/** Everything the application needs from the storage. Failures are [[VotingError]]s. */
@ImplementedBy(classOf[JdbcVotingRepository])
trait VotingRepository:

  /** The open event with its candidates. */
  def currentEvent(): Future[Event]

  /** Opens a new event (closing the previous one) together with all its candidates, atomically. */
  def createEvent(event: NewEvent): Future[Event]

  /** Records a vote, enforcing the per-voter limit. */
  def castVote(ballot: Ballot): Future[Unit]

  /** Running totals of an event, by default the open one. */
  def results(eventId: Option[Int] = None): Future[List[CandidateVotes]]

  /** The open event and its totals, read consistently. */
  def snapshot(): Future[Snapshot]

object VotingRepository:
  /** How many times a single voter may vote in an event. */
  val MaxVotesPerVoter = 3
