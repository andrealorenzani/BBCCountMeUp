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

package name.lorenzani.andrea.bbc

import name.lorenzani.andrea.bbc.model.*
import name.lorenzani.andrea.bbc.model.JsonFormats.given
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.*

class JsonFormatsSpec extends AnyFunSuite with Matchers:

  test("an event is written with its candidates") {
    val event = Event(1, "Best song", "Vote!", List(Candidate(10, "A")))
    Json.toJson(event) shouldBe Json.parse(
      """{"id":1,"name":"Best song","description":"Vote!","candidates":[{"id":10,"name":"A"}]}"""
    )
  }

  test("a snapshot nests the event and the results") {
    val snapshot = Snapshot(Event(1, "E", "D", Nil), List(CandidateVotes("A", 3)))
    (Json.toJson(snapshot) \ "results").as[JsArray] shouldBe Json.parse("""[{"name":"A","votes":3}]""")
    (Json.toJson(snapshot) \ "event" \ "name").as[String] shouldBe "E"
  }

  test("initial votes default to zero when omitted") {
    val json = Json.parse("""{"name":"E","description":"D","candidates":[{"name":"A"},{"name":"B","initialVotes":7}]}""")
    json.as[NewEvent].candidates shouldBe List(NewCandidate("A", 0), NewCandidate("B", 7))
  }

  test("a ballot needs voter, candidateId and eventId") {
    Json.parse("""{"voter":"v","candidateId":2,"eventId":1}""").as[Ballot] shouldBe Ballot("v", 2, 1)
    Json.parse("""{"voter":"v","candidateId":2}""").validate[Ballot].isError shouldBe true
  }

  test("wrongly typed fields are rejected") {
    Json.parse("""{"name":"E","description":"D","candidates":[{"name":"A","initialVotes":"many"}]}""").validate[NewEvent].isError shouldBe true
  }
