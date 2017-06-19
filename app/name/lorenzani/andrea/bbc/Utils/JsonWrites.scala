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

package name.lorenzani.andrea.bbc.Utils

import name.lorenzani.andrea.bbc.database.DbAccess.{Candidate, Event, Votes}
import play.api.libs.json.{JsPath, Writes}
import play.api.libs.functional.syntax._

object JsonWrites {
  // Ref: https://www.playframework.com/documentation/2.5.x/ScalaJsonCombinators
  implicit val candidateWrites: Writes[Candidate] = (
    (JsPath \ "id").write[Int] and
      (JsPath \ "name").write[String]
    ) (unlift(Candidate.unapply))

  implicit val voteWrites: Writes[Votes] = (
    (JsPath \ "label").write[String] and
      (JsPath \ "y").write[Long]
    ) (unlift(Votes.unapply))

  implicit val eventWrites: Writes[Event] = (
    (JsPath \ "id").write[Int] and
    (JsPath \ "name").write[String] and
    (JsPath \ "desc").write[String] and
    (JsPath \ "candidates").write[List[Candidate]]
    ) (unlift(Event.unapply))

}
