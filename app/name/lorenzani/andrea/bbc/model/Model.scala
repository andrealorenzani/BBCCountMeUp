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

package name.lorenzani.andrea.bbc.model

import scala.util.control.NoStackTrace

final case class Candidate(id: Int, name: String)

final case class Event(id: Int, name: String, description: String, candidates: List[Candidate])

/** Votes collected so far by one candidate. */
final case class CandidateVotes(name: String, votes: Long)

/** What the presenter shows: the current event together with its running totals. */
final case class Snapshot(event: Event, results: List[CandidateVotes])

final case class NewCandidate(name: String, initialVotes: Int = 0)

final case class NewEvent(name: String, description: String, candidates: List[NewCandidate])

object NewEvent:
  val MinCandidates = 2

  /** Business rules for a new event; all the violations are reported at once. */
  def validate(event: NewEvent): Either[List[String], NewEvent] =
    val names = event.candidates.map(_.name.trim)
    val errors = List(
      Option.when(event.name.isBlank)("The event needs a name"),
      Option.when(event.description.isBlank)("The event needs a description"),
      Option.when(event.candidates.sizeIs < MinCandidates)(s"At least $MinCandidates candidates are required"),
      Option.when(names.exists(_.isEmpty))("Candidate names cannot be blank"),
      Option.when(names.distinct.sizeIs != names.size)("Candidate names must be unique"),
      Option.when(event.candidates.exists(_.initialVotes < 0))("Initial votes cannot be negative")
    ).flatten
    Either.cond(errors.isEmpty, event, errors)

/** A single vote cast by `voter` for a candidate of the event it believes to be open. */
final case class Ballot(voter: String, candidateId: Int, eventId: Int)

/** Domain failures; the controllers translate them to HTTP statuses. */
sealed abstract class VotingError(message: String) extends RuntimeException(message) with NoStackTrace

object VotingError:
  case object NoOpenEvent extends VotingError("No event is open yet")
  final case class UnknownCandidate(id: Int) extends VotingError(s"Candidate $id does not exist in the open event")
  final case class StaleEvent(requested: Int, current: Int)
      extends VotingError(s"Event $requested is not the open event anymore")
  final case class VoteLimitReached(voter: String, limit: Int)
      extends VotingError(s"$voter has already cast the maximum number of votes ($limit)")
