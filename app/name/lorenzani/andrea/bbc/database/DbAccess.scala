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

package name.lorenzani.andrea.bbc.database

import java.sql.Connection

import play.api.db.Database

import scala.util.{Failure, Success, Try}

object DbAccess {

  case class Votes(name: String, nvotes: Long)

  case class Candidate(id: Int, name: String)

  case class Event(id: Int, name: String, desc: String, candidates: List[Candidate])

}

// For simplicity I define here all the queries I need. This is a trait, I think I will need
// 3 actors for the access: one that is able to retrieve the result of the vote, one for
// the admin part and one for the voter part.
// Ref for db in Play: https://www.playframework.com/documentation/2.5.x/ScalaDatabase
trait DbAccess {

  import DbAccess._

  val db: Database

  protected def currEventId(openconn: Connection): Try[Int] = {
    val selectEvent = "SELECT ID FROM EVENTS WHERE CURRENT = TRUE;"
    val rs = openconn.createStatement().executeQuery(selectEvent)
    if (rs.next()) {
      Success(rs.getInt("ID"))
    }
    else Failure(new NoSuchElementException("No event open yet"))
  }

  protected def insertVote(voter: String, candidate: Int): Unit = {
    // For simplicity I will avoid registering voters, so the voter is a String but it should be his/her id (Int)
    db.withConnection(autocommit = false) { conn =>
      val votesStm = conn.prepareStatement("SELECT COUNT(*) AS VOTES FROM VOTES WHERE VOTER = ? AND EVENTID = ?;")
      val candidateStm = conn.prepareStatement("SELECT NAME FROM CANDIDATES WHERE ID = ?;")
      val insertStm = conn.prepareStatement("INSERT INTO VOTES(VOTER, EVENTID, CANDIDATEID, CANDIDATENAME) VALUES(?, ?, ?, ?);")
      val updateStm = conn.prepareStatement("UPDATE CACHE SET VOTES=VOTES+1 WHERE EVENTID=? AND CANDIDATEID=?;")

      val eventId = currEventId(conn).get
      // It is much easier to make the voter wait a bit more by checking if its vote
      // has to be ignored and have a straightforward query for the result that not
      // filtering the votes during the computation of the result
      votesStm.setString(1, voter)
      votesStm.setInt(2, eventId)
      val votesRS = votesStm.executeQuery()
      if (!votesRS.next() || votesRS.getInt("VOTES") < 3) {
        candidateStm.setInt(1, candidate)
        val candidRS = candidateStm.executeQuery()
        if (candidRS.next()) {
          val candName = candidRS.getString("NAME")
          insertStm.setString(1, voter)
          // It is perfectly fine to raise an error if no event is open
          insertStm.setInt(2, eventId)
          // Please consider that I assume that a person can make 3 votes for the same candidate
          insertStm.setInt(3, candidate)
          insertStm.setString(4, candName)
          insertStm.executeUpdate()
          updateStm.setInt(1, eventId)
          updateStm.setInt(2, candidate)
          updateStm.executeUpdate()
          conn.commit()
        }
        else throw new NoSuchElementException("Candidate does not exist")
      }
      else throw new IllegalArgumentException("Maximum number of votes already reached")
    }
  }

  protected def getVoteResult(eventId: Option[Int] = None): List[Votes] = {
    db.withConnection(autocommit = true) { conn =>
      // I did a last minute optimization here, so I could have normalized the database
      // Before it was a long lasting query and not joining improved the performance
      val votesStm = conn.prepareStatement("SELECT CANDIDATENAME AS CANDIDATE, VOTES FROM CACHE WHERE EVENTID=?;")
      // It is perfectly normal to raise an exception if no event is currently open
      votesStm.setInt(1, eventId.getOrElse(currEventId(conn).get))
      val voteRS = votesStm.executeQuery()
      new Iterator[Votes] {
        def hasNext = voteRS.next()

        def next() = Votes(voteRS.getString("CANDIDATE"), voteRS.getLong("VOTES"))
      }.toList
    }
  }

  protected def openEvent(name: String, description: String, current: Boolean): Unit = {
    // This create an event for a vote - Should be in an Admin console
    db.withConnection(autocommit = true) { conn =>
      val insertStm = conn.prepareStatement("INSERT INTO EVENTS(NAME, DESCRIPTION, CURRENT) VALUES(?, ?, ?);")
      val updateStm = conn.prepareStatement("UPDATE EVENTS SET CURRENT = FALSE WHERE CURRENT = TRUE;")
      if (current) {
        updateStm.executeUpdate()
      }
      insertStm.clearParameters()
      insertStm.setString(1, name)
      insertStm.setString(2, description)
      insertStm.setBoolean(3, current)
      insertStm.executeUpdate()
    }
  }

  protected def getCurrentEvent: Event = {
    // Returns the current event with all its details and the associated candidates
    db.withConnection(autocommit = true) { conn =>
      val oldStm = "SELECT E.ID AS ID, E.NAME AS NAME, E.DESCRIPTION AS DESCRIPTION, C.ID AS CANDID, C.NAME AS CANDIDATE FROM EVENTS E LEFT JOIN CANDIDATES C ON E.ID = C.EVENTID WHERE E.CURRENT = TRUE;"
      val rs = conn.createStatement().executeQuery(oldStm)
      val evinfo = new Iterator[(Int, String, String, Candidate)] {
        def hasNext = rs.next()

        def next() = (rs.getInt("ID"), rs.getString("NAME"), rs.getString("DESCRIPTION"), Candidate(rs.getInt("CANDID"), rs.getString("CANDIDATE")))
      }.toStream
      if (evinfo.isEmpty) throw new NoSuchElementException("No event open yet")
      evinfo.foldLeft(Event(-1, "", "", List())) { case (ev, (evid, evname, evdesc, cand)) =>
        if (cand.name == null && cand.id == 0) {
          // It is the DB way to do the left join
          Event(evid, evname, evdesc, ev.candidates)
        }
        else {
          Event(evid, evname, evdesc, cand :: ev.candidates)
        }
      }
    }
  }

  protected def insertCandidate(name: String, initialVotes: Int = 0, eventId: Option[Int] = None): Unit = {
    db.withConnection(autocommit = false) { conn =>
      val insertStm = conn.prepareStatement("INSERT INTO CANDIDATES (EVENTID, NAME) VALUES (?, ?);")
      // It is perfectly fine to throw an error if we want to add a candidate without an event
      val event = eventId.getOrElse(currEventId(conn).get)
      insertStm.setInt(1, event)
      insertStm.setString(2, name)
      insertStm.executeUpdate()

      val candidateStm = conn.prepareStatement("SELECT ID, NAME FROM CANDIDATES WHERE NAME = ? AND EVENTID = ?;")
      candidateStm.setString(1, name)
      candidateStm.setInt(2, event)
      val rs = candidateStm.executeQuery()
      while (rs.next()) {
        val candId = rs.getInt("ID")
        val voteStm = conn.prepareStatement("INSERT INTO CACHE(EVENTID, CANDIDATEID, CANDIDATENAME, VOTES) VALUES(?, ?, ?, ?);")
        voteStm.clearParameters()
        voteStm.setInt(1, event)
        voteStm.setInt(2, candId)
        voteStm.setString(3, name)
        voteStm.setInt(4, initialVotes)
        voteStm.executeUpdate()
      }
      conn.commit()
    }
  }
}
