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

package name.lorenzani.andrea.bbc.repository

import java.sql.{Connection, PreparedStatement, ResultSet, Statement}

import jakarta.inject.{Inject, Singleton}
import name.lorenzani.andrea.bbc.model.*
import name.lorenzani.andrea.bbc.model.VotingError.*
import play.api.db.Database

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

@Singleton
class JdbcVotingRepository @Inject() (db: Database, databaseEc: DatabaseExecutionContext) extends VotingRepository:

  private given ExecutionContext = databaseEc

  // Counting a voter's ballots and inserting the next one must not interleave. A lock is enough
  // because the application runs on a single node; with several nodes a database-level lock
  // (SELECT ... FOR UPDATE on a per-voter row) would be needed.
  private val ballotLock = Object()

  override def currentEvent(): Future[Event] =
    Future(db.withConnection(readCurrentEvent))

  override def createEvent(event: NewEvent): Future[Event] = Future {
    db.withTransaction { conn =>
      update(conn, "UPDATE EVENTS SET IS_CURRENT = FALSE WHERE IS_CURRENT = TRUE")()
      val eventId = insert(conn, "INSERT INTO EVENTS(NAME, DESCRIPTION, IS_CURRENT) VALUES(?, ?, TRUE)")(
        _.setString(1, event.name.trim),
        _.setString(2, event.description.trim)
      )
      event.candidates.foreach { candidate =>
        val name = candidate.name.trim
        val candidateId = insert(conn, "INSERT INTO CANDIDATES(EVENTID, NAME) VALUES(?, ?)")(
          _.setInt(1, eventId),
          _.setString(2, name)
        )
        update(conn, "INSERT INTO CACHE(EVENTID, CANDIDATEID, CANDIDATENAME, VOTES) VALUES(?, ?, ?, ?)")(
          _.setInt(1, eventId),
          _.setInt(2, candidateId),
          _.setString(3, name),
          _.setInt(4, candidate.initialVotes)
        )
      }
      readCurrentEvent(conn)
    }
  }

  override def castVote(ballot: Ballot): Future[Unit] = Future {
    ballotLock.synchronized {
      db.withTransaction { conn =>
        val event = readCurrentEvent(conn)
        if event.id != ballot.eventId then throw StaleEvent(ballot.eventId, event.id)
        val candidate = event.candidates.find(_.id == ballot.candidateId).getOrElse(throw UnknownCandidate(ballot.candidateId))

        val votesSoFar = query(conn, "SELECT COUNT(*) FROM VOTES WHERE EVENTID = ? AND VOTER = ?")(
          _.setInt(1, event.id),
          _.setString(2, ballot.voter)
        )(_.getInt(1)).head
        if votesSoFar >= VotingRepository.MaxVotesPerVoter then
          throw VoteLimitReached(ballot.voter, VotingRepository.MaxVotesPerVoter)

        update(conn, "INSERT INTO VOTES(VOTER, EVENTID, CANDIDATEID, CANDIDATENAME) VALUES(?, ?, ?, ?)")(
          _.setString(1, ballot.voter),
          _.setInt(2, event.id),
          _.setInt(3, candidate.id),
          _.setString(4, candidate.name)
        )
        update(conn, "UPDATE CACHE SET VOTES = VOTES + 1 WHERE EVENTID = ? AND CANDIDATEID = ?")(
          _.setInt(1, event.id),
          _.setInt(2, candidate.id)
        )
      }
    }
  }

  override def results(eventId: Option[Int]): Future[List[CandidateVotes]] =
    Future(db.withConnection(conn => readResults(conn, eventId.getOrElse(currentEventId(conn)))))

  override def snapshot(): Future[Snapshot] = Future {
    db.withTransaction { conn =>
      val event = readCurrentEvent(conn)
      Snapshot(event, readResults(conn, event.id))
    }
  }

  private def currentEventId(conn: Connection): Int =
    query(conn, "SELECT ID FROM EVENTS WHERE IS_CURRENT = TRUE")()(_.getInt(1)).headOption.getOrElse(throw NoOpenEvent)

  private def readCurrentEvent(conn: Connection): Event =
    val eventId = currentEventId(conn)
    val (name, description) = query(conn, "SELECT NAME, DESCRIPTION FROM EVENTS WHERE ID = ?")(_.setInt(1, eventId)) { rs =>
      (rs.getString("NAME"), rs.getString("DESCRIPTION"))
    }.head
    val candidates = query(conn, "SELECT ID, NAME FROM CANDIDATES WHERE EVENTID = ? ORDER BY ID")(_.setInt(1, eventId)) { rs =>
      Candidate(rs.getInt("ID"), rs.getString("NAME"))
    }
    Event(eventId, name, description, candidates)

  private def readResults(conn: Connection, eventId: Int): List[CandidateVotes] =
    query(conn, "SELECT CANDIDATENAME, VOTES FROM CACHE WHERE EVENTID = ? ORDER BY CANDIDATEID")(_.setInt(1, eventId)) { rs =>
      CandidateVotes(rs.getString("CANDIDATENAME"), rs.getLong("VOTES"))
    }

  // --- tiny JDBC helpers: every statement and result set is closed ---

  private type Param = PreparedStatement => Unit

  private def query[A](conn: Connection, sql: String)(params: Param*)(read: ResultSet => A): List[A] =
    Using.resource(conn.prepareStatement(sql)) { statement =>
      params.foreach(_(statement))
      Using.resource(statement.executeQuery()) { rs =>
        val rows = ListBuffer.empty[A]
        while rs.next() do rows += read(rs)
        rows.toList
      }
    }

  private def update(conn: Connection, sql: String)(params: Param*): Unit =
    Using.resource(conn.prepareStatement(sql)) { statement =>
      params.foreach(_(statement))
      statement.executeUpdate()
    }

  /** Runs an INSERT and returns the generated id. */
  private def insert(conn: Connection, sql: String)(params: Param*): Int =
    Using.resource(conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { statement =>
      params.foreach(_(statement))
      statement.executeUpdate()
      Using.resource(statement.getGeneratedKeys) { keys =>
        keys.next()
        keys.getInt(1)
      }
    }
