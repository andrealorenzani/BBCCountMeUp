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

import name.lorenzani.andrea.bbc.database.{AdminDB, PresenterDB, VoterDB}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.{AfterAll, BeforeAll}
import play.api.db.evolutions.Evolutions
import play.api.db.{Database, Databases}
import play.api.test.PlaySpecification

import scala.util.Try

@RunWith(classOf[JUnitRunner])
class VoterDbAccessSpec  extends PlaySpecification with BeforeAll with AfterAll {

  var db: Option[Database] = None

  "VoterDB access database" should {

    "be able to insert Data" in {
      val adminDb = new AdminDB(db.get)
      val evName = "First Event"
      val evDesc = "This is a test event"
      adminDb.openEvent(evName, evDesc, true)
      adminDb.insertCandidate("Candidate", 0)
      adminDb.insertCandidate("OtherCandidate", 0)
      adminDb.insertCandidate("OtherOtherCandidate", 0)
      adminDb.insertCandidate("UselessCandidate", 0)
      val event = adminDb.getCurrentEvent
      val voterDb = new VoterDB(db.get)
      // we test also that the voter cannot put 4 votes
      event.candidates.foreach{ case c =>
        (1 to 4).foreach { x =>
          val insert = Try{voterDb.insertVote(s"${c.name}_supporter", c.id)}
          if(x < 4) insert must beASuccessfulTry
          else insert must beAFailedTry
        }
      }
      val presenterDb = new PresenterDB(db.get)
      val res = presenterDb.getVoteResult()
      res.map(x => x.name) must containTheSameElementsAs(List("Candidate", "OtherCandidate", "OtherOtherCandidate", "UselessCandidate"))
      res.map(x => x.nvotes) must containTheSameElementsAs(List(3, 3, 3, 3))
    }
  }

  override def beforeAll(): Unit = {
    db = Some(Databases.inMemory())
    Evolutions.applyEvolutions(db.get)
  }

  override def afterAll(): Unit = db.map(_.shutdown())

}
