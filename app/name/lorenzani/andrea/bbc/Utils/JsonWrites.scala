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
