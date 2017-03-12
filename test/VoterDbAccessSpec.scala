import name.lorenzani.andrea.bbc.database.{AdminDB, PresenterDB, VoterDB}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.{AfterAll, BeforeAll}
import play.api.db.evolutions.Evolutions
import play.api.db.{Database, Databases}
import play.api.test.PlaySpecification

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
      (1 to 4).foreach( x => event.candidates.foreach(c => voterDb.insertVote(s"me$x", c.id)))
      val presenterDb = new PresenterDB(db.get)
      val res = presenterDb.getVoteResult()
      res.map(x => x.name) must containTheSameElementsAs(List("Candidate", "OtherCandidate", "OtherOtherCandidate", "UselessCandidate"))
      res.map(x => x.nvotes) must containTheSameElementsAs(List(3, 3, 3, 0))
    }
  }

  override def beforeAll(): Unit = {
    db = Some(Databases.inMemory())
    Evolutions.applyEvolutions(db.get)
  }

  override def afterAll(): Unit = db.map(_.shutdown())

}
