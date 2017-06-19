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
import play.api.libs.json.{JsNumber, JsPath, Reads}
import play.api.libs.functional.syntax._

// Ref: https://www.playframework.com/documentation/2.5.x/ScalaJsonCombinators
object JsonReads {

  implicit val eventRead: Reads[Event] = (
    (JsPath \ "id").read[Int] and
    (JsPath \ "eventname").read[String] and
    (JsPath \ "description").read[String] and
    (JsPath \ "candidates").read[List[Candidate]]
    )(Event.apply _)

  implicit val candidateRead: Reads[Candidate] = (
    (JsPath \ "id").read[Int] and
      (JsPath \ "name").read[String]
    )(Candidate.apply _)

  implicit val votesRead: Reads[Votes] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "nvotes").read[Long]
    )(Votes.apply _)

  implicit val adminCandRead: Reads[(String, Int)] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "initialVotes").read[Int]
    ).tupled

  implicit val adminFormRead: Reads[(Event, List[Votes])] = (
    (JsPath \ "eventname").read[String] and
    (JsPath \ "description").read[String] and
    (JsPath \ "candidates").read[List[(String, Int)]]
  ){ (evname, desc, cand) => (Event(-1, evname, desc, List()), cand.map(x=>Votes(x._1, x._2))) }

  implicit val voteFromVoter: Reads[(String, Int, Int)] = (
    (JsPath \ "voter").read[String] and
      (JsPath \ "candidate").read[Int] and
      (JsPath \ "eventid").read[Int]
    ).tupled

}
