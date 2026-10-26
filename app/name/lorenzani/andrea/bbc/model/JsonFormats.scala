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

import play.api.libs.json.*

/** The JSON wire format of the REST API. */
object JsonFormats:
  given Writes[Candidate] = Json.writes[Candidate]
  given Writes[Event] = Json.writes[Event]
  given Writes[CandidateVotes] = Json.writes[CandidateVotes]
  given Writes[Snapshot] = Json.writes[Snapshot]

  given Reads[NewCandidate] = Reads { json =>
    for
      name <- (json \ "name").validate[String]
      votes <- (json \ "initialVotes").validateOpt[Int].map(_.getOrElse(0))
    yield NewCandidate(name, votes)
  }
  given Reads[NewEvent] = Json.reads[NewEvent]
  given Reads[Ballot] = Json.reads[Ballot]
