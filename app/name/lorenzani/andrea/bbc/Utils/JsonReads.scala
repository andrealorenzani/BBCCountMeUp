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

  implicit val adminFormRead: Reads[(Event, List[Votes])] = (
    (JsPath \ "eventname").read[String] and
    (JsPath \ "description").read[String] and
    (JsPath \ "candidates").read[List[(String, Int)]]
  ){ (evname, desc, cand) => (Event(-1, evname, desc, List()), cand.map(x=>Votes(x._1, x._2))) }

  implicit val adminCandRead: Reads[(String, Int)] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "initialVotes").read[Int]
    ).tupled

}
