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
class EmptyDbAccessSpec  extends PlaySpecification with BeforeAll with AfterAll {

  var db: Option[Database] = None

  "Database should alert of no event open by throwing an exception" in {
    val adminDb = new AdminDB(db.get)
    val currEvent = Try{ adminDb.getCurrentEvent }
    currEvent must beAFailedTry
    val voterDb = new VoterDB(db.get)
    val currVote = Try{ voterDb.insertVote("Micky Mouse", 1) }
    currVote must beAFailedTry
    val presenterDb = new PresenterDB(db.get)
    val currRes = Try{ presenterDb.getVoteResult() }
    currRes must beAFailedTry
  }


  override def beforeAll(): Unit = {
    db = Some(Databases.inMemory())
    Evolutions.applyEvolutions(db.get)
  }

  override def afterAll(): Unit = db.foreach(_.shutdown())

}

