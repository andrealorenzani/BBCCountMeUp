import name.lorenzani.andrea.bbc.database.{AdminDB, PresenterDB}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.{AfterAll, BeforeAll}
import play.api.db.evolutions.Evolutions
import play.api.db.{Database, Databases}
import play.api.test.PlaySpecification


@RunWith(classOf[JUnitRunner])
class AdminDbAccessSpec  extends PlaySpecification with BeforeAll with AfterAll {

  var db: Option[Database] = None

  def verifyIsTheCurrentEvent(name: String): Boolean = {
    db.get.withConnection(autocommit = true){ conn =>
      val rs = conn.createStatement().executeQuery("SELECT NAME FROM EVENTS WHERE CURRENT = TRUE;")
      rs.next()
      if(rs.getString("NAME") != name || rs.next()) {
        false
      }
      else {
        true
      }
    }
  }

  "AdminDB access database" should {

    "be able to create and retrieve the current event" in {
      val adminDb = new AdminDB(db.get)
      val evName = "First Event"
      val evDesc = "This is a test event"
      adminDb.openEvent(evName, evDesc, true)
      val currEvent = adminDb.getCurrentEvent
      currEvent.name must equalTo(evName)
      currEvent.desc must equalTo(evDesc)
      currEvent.candidates must equalTo(List.empty)
    }

    "be able to create an event and add multiple candidates" in {
      val adminDb = new AdminDB(db.get)
      val evName = "Second Event"
      val evDesc = "This is a test event"
      val candidates = List("Tizio", "Caio", "Sempronio")
      adminDb.openEvent(evName, evDesc, true)
      verifyIsTheCurrentEvent(evName) must beTrue
      candidates.foreach(adminDb.insertCandidate(_))
      val currEvent = adminDb.getCurrentEvent
      currEvent.candidates.map(_.name) must containTheSameElementsAs(candidates)
    }

    "be able to create an event and add multiple candidates with votes" in {
      val adminDb = new AdminDB(db.get)
      val evName = "Third Event"
      val evDesc = "This is a test event"
      val candidates = List("Tizio", "Caio", "Sempronio")
      adminDb.openEvent(evName, evDesc, true)
      verifyIsTheCurrentEvent(evName) must beTrue
      candidates.zipWithIndex.foreach { case (cand, ind) => adminDb.insertCandidate(cand, ind) }
      val presenterDb = new PresenterDB(db.get)
      val votes = presenterDb.getVoteResult()
      votes.map(_.name) must containTheSameElementsAs(candidates)
      votes.map(x => (x.name, x.nvotes)) must containTheSameElementsAs(candidates.zipWithIndex)
    }
  }

  override def beforeAll(): Unit = {
    db = Some(Databases.inMemory())
    Evolutions.applyEvolutions(db.get)
  }

  override def afterAll(): Unit = db.map(_.shutdown())

}
