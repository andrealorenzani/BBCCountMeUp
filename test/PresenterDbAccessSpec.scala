import name.lorenzani.andrea.bbc.database.{AdminDB, PresenterDB}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.specification.{AfterAll, BeforeAll}
import play.api.db.evolutions.Evolutions
import play.api.db.{Database, Databases}
import play.api.test.PlaySpecification

@RunWith(classOf[JUnitRunner])
class PresenterDbAccessSpec  extends PlaySpecification with BeforeAll with AfterAll {

  var db: Option[Database] = None

  "PresenterDB access database" should {

    "be able to see data" in {
      val adminDb = new AdminDB(db.get)
      val evName = "First Event"
      val evDesc = "This is a test event"
      adminDb.openEvent(evName, evDesc, true)
      adminDb.insertCandidate("Candidate", 10)
      adminDb.insertCandidate("OtherCandidate", 20)
      val presenterDb = new PresenterDB(db.get)
      val res = presenterDb.getVoteResult()
      res.map(x => x.name) must containTheSameElementsAs(List("Candidate", "OtherCandidate"))
      res.map(x => x.nvotes) must containTheSameElementsAs(List(10, 10))
    }
  }

  override def beforeAll(): Unit = {
    db = Some(Databases.inMemory())
    Evolutions.applyEvolutions(db.get)
  }

  override def afterAll(): Unit = db.map(_.shutdown())

}
