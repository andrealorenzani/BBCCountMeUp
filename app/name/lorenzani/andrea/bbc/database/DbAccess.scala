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
    if(rs.next()) {
      Success(rs.getInt("ID"))
    }
    else Failure(new NoSuchElementException("No event open yet"))
  }

  protected def insertVote(voter: String, candidate: Int): Unit = {
    // For simplicity I will avoid registering voters, so the voter is a String but it should be his/her id (Int)
    db.withConnection(autocommit = true) { conn =>
      val votesStm = conn.prepareStatement("SELECT COUNT(*) AS VOTES FROM VOTES WHERE VOTER = ? AND EVENTID = ?;")
      val candidateStm = conn.prepareStatement("SELECT NAME FROM CANDIDATES WHERE ID = ?;")
      val insertStm = conn.prepareStatement("INSERT INTO VOTES(VOTER, EVENTID, CANDIDATEID, CANDIDATENAME) VALUES(?, ?, ?, ?);")

      val eventId = currEventId(conn).get
      // It is much easier to make the voter wait a bit more by checking if its vote
      // has to be ignored and have a straightforward query for the result that not
      // filtering the votes during the computation of the result
      votesStm.setString(1, voter)
      votesStm.setInt(2, eventId)
      val votesRS = votesStm.executeQuery()
      if(!votesRS.next() || votesRS.getInt("VOTES") < 3) {
        candidateStm.setInt(1, candidate)
        val candidRS = candidateStm.executeQuery()
        if(candidRS.next()) {
          val candName = candidRS.getString("NAME")
          insertStm.setString(1, voter)
          // It is perfectly fine to raise an error if no event is open
          insertStm.setInt(2, eventId)
          // Please consider that I assume that a person can make 3 votes for the same candidate
          insertStm.setInt(3, candidate)
          insertStm.setString(4, candName)
          insertStm.executeUpdate()
        }
        else throw new NoSuchElementException("Candidate does not exist")
      }
      else throw new IllegalArgumentException("Maximum number of votes already reached")
    }
  }

  protected def getVoteResult(eventId: Option[Int] = None): List[Votes] = {
    db.withConnection(autocommit = true) { conn =>
      // I don't like JOIN statements for high performance query, they are time consuming
      // I prefer not normalized databases
      val votesStm = conn.prepareStatement("SELECT CANDIDATENAME AS CANDIDATE, COUNT(*) AS VOTES FROM VOTES WHERE EVENTID=? GROUP BY CANDIDATEID;")
      val candStm = conn.prepareStatement("SELECT NAME FROM CANDIDATES WHERE EVENTID=?;")
      // It is perfectly normal to raise an exception if no event is currently open
      votesStm.setInt(1, eventId.getOrElse(currEventId(conn).get))
      val voteRS = votesStm.executeQuery()
      val votes = new Iterator[(String, Long)] {
        def hasNext = voteRS.next()
        def next() = (voteRS.getString("CANDIDATE"), voteRS.getLong("VOTES"))
      }.toMap
      candStm.setInt(1, eventId.getOrElse(currEventId(conn).get))
      val candRs = candStm.executeQuery()
      new Iterator[String] {
        def hasNext = candRs.next()
        def next() = candRs.getString("NAME")
      }.toList.map { cand =>
        Votes(cand, votes.getOrElse(cand, 0l))
      }
    }
  }

  protected def openEvent(name: String, description: String, current: Boolean): Unit = {
    // This create an event for a vote - Should be in an Admin console
    db.withConnection(autocommit = true) { conn =>
      val insertStm = conn.prepareStatement("INSERT INTO EVENTS(NAME, DESCRIPTION, CURRENT) VALUES(?, ?, ?);")
      val updateStm = conn.prepareStatement("DELETE EVENTS WHERE CURRENT = ?;")
      if(current) {
        updateStm.setBoolean(1, true)
        //updateStm.setBoolean(2, true)
        val updated = updateStm.executeUpdate()
        print(updated)
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
      if(evinfo.isEmpty) throw new NoSuchElementException("No event open yet")
      evinfo.foldLeft(Event(-1, "", "", List())) { case (ev, (evid, evname, evdesc, cand)) =>
        if(cand.name == null && cand.id == 0) {
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
      if(initialVotes>0) {
        // This can have problems with duplicate names - I hope admins won't put twice the exact same name
        val candidateStm = conn.prepareStatement("SELECT ID, NAME FROM CANDIDATES WHERE NAME = ? AND EVENTID = ?;")
        candidateStm.setString(1, name)
        candidateStm.setInt(2, event)
        val rs = candidateStm.executeQuery()
        if(rs.next()) {
          val candId = rs.getInt("ID")
          val voteStm = conn.prepareStatement("INSERT INTO VOTES(VOTER, EVENTID, CANDIDATEID, CANDIDATENAME) VALUES(?, ?, ?, ?);")
          (1 to initialVotes).map { voteNum =>
            val voter = s"automatic_voter_${voteNum/3}"
            voteStm.clearParameters()
            voteStm.setString(1, voter)
            voteStm.setInt(2, event)
            voteStm.setInt(3, candId)
            voteStm.setString(4, name)
            voteStm.executeUpdate()
          }
        }
      }
      conn.commit()
    }
  }
}
