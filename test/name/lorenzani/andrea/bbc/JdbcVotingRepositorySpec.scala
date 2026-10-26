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

package name.lorenzani.andrea.bbc

import name.lorenzani.andrea.bbc.model.*
import name.lorenzani.andrea.bbc.model.VotingError.*
import name.lorenzani.andrea.bbc.repository.{JdbcVotingRepository, VotingRepository}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers

import scala.concurrent.{ExecutionContext, Future}

class JdbcVotingRepositorySpec extends AnyWordSpec with Matchers with ScalaFutures with GuiceOneAppPerSuite:

  override given patienceConfig: PatienceConfig = PatienceConfig(Span(10, Seconds), Span(25, Millis))

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(Helpers.inMemoryDatabase()).build()

  private given ExecutionContext = ExecutionContext.global

  private lazy val repository: JdbcVotingRepository = app.injector.instanceOf[JdbcVotingRepository]

  private def openEvent(name: String = "Event", candidates: (String, Int)*): Event =
    val cands = if candidates.isEmpty then List(NewCandidate("Tizio"), NewCandidate("Caio")) else candidates.map(NewCandidate(_, _)).toList
    repository.createEvent(NewEvent(name, "A test event", cands)).futureValue

  private def vote(voter: String, candidate: Candidate, event: Event): Future[Unit] =
    repository.castVote(Ballot(voter, candidate.id, event.id))

  "The repository" should {

    "be wired by Guice to the trait" in {
      app.injector.instanceOf[VotingRepository] shouldBe a[JdbcVotingRepository]
    }

    "open an event with its candidates, in insertion order" in {
      val event = openEvent("First", "Tizio" -> 0, "Caio" -> 0, "Sempronio" -> 0)
      event.name shouldBe "First"
      event.description shouldBe "A test event"
      event.candidates.map(_.name) shouldBe List("Tizio", "Caio", "Sempronio")
      repository.currentEvent().futureValue shouldBe event
    }

    "trim names when storing them" in {
      val event = repository.createEvent(NewEvent(" Padded ", " d ", List(NewCandidate(" A "), NewCandidate("B")))).futureValue
      event.name shouldBe "Padded"
      event.candidates.map(_.name) shouldBe List("A", "B")
    }

    "start the results from the initial votes" in {
      openEvent("Seeded", "Tizio" -> 0, "Caio" -> 1, "Sempronio" -> 2)
      repository.results().futureValue shouldBe List(CandidateVotes("Tizio", 0), CandidateVotes("Caio", 1), CandidateVotes("Sempronio", 2))
    }

    "close the previous event when a new one is opened" in {
      val first = openEvent("One")
      val second = openEvent("Two")
      second.id should not be first.id
      repository.currentEvent().futureValue.name shouldBe "Two"
      // the old totals are still available by id
      repository.results(Some(first.id)).futureValue.map(_.name) shouldBe List("Tizio", "Caio")
    }

    "count a vote in the results" in {
      val event = openEvent()
      vote("alice", event.candidates.head, event).futureValue
      vote("bob", event.candidates.head, event).futureValue
      vote("carol", event.candidates(1), event).futureValue
      repository.results().futureValue shouldBe List(CandidateVotes("Tizio", 2), CandidateVotes("Caio", 1))
    }

    "let a voter vote three times, also for the same candidate, but not four" in {
      val event = openEvent()
      (1 to 3).foreach(_ => vote("alice", event.candidates.head, event).futureValue)
      vote("alice", event.candidates(1), event).failed.futureValue shouldBe VoteLimitReached("alice", 3)
      // someone else is not affected, and the rejected vote was not counted
      vote("bob", event.candidates(1), event).futureValue
      repository.results().futureValue shouldBe List(CandidateVotes("Tizio", 3), CandidateVotes("Caio", 1))
    }

    "apply the limit per event" in {
      val first = openEvent("One")
      (1 to 3).foreach(_ => vote("alice", first.candidates.head, first).futureValue)
      val second = openEvent("Two")
      vote("alice", second.candidates.head, second).futureValue
    }

    "never exceed the limit under concurrent votes of the same voter" in {
      val event = openEvent()
      val attempts = Future.traverse(1 to 20)(_ => vote("eager", event.candidates.head, event).transform(scala.util.Success(_)))
      attempts.futureValue.count(_.isSuccess) shouldBe 3
      repository.results().futureValue.head.votes shouldBe 3
    }

    "reject a candidate that is not part of the open event" in {
      val old = openEvent("Old")
      val event = openEvent("New")
      vote("alice", old.candidates.head, event).failed.futureValue shouldBe a[UnknownCandidate]
      repository.castVote(Ballot("alice", 99999, event.id)).failed.futureValue shouldBe UnknownCandidate(99999)
    }

    "reject a vote for an event that is not open anymore" in {
      val old = openEvent("Old")
      val event = openEvent("New")
      vote("alice", old.candidates.head, old).failed.futureValue shouldBe StaleEvent(old.id, event.id)
    }

    "give a consistent snapshot" in {
      val event = openEvent("Snap", "Tizio" -> 4, "Caio" -> 0)
      repository.snapshot().futureValue shouldBe Snapshot(event, List(CandidateVotes("Tizio", 4), CandidateVotes("Caio", 0)))
    }

    "roll back everything when an event cannot be stored" in {
      val before = openEvent("Stable")
      // a name longer than the column makes the second insert fail after the event row was written
      val tooLong = "x" * 300
      repository.createEvent(NewEvent("Broken", "d", List(NewCandidate("A"), NewCandidate(tooLong)))).failed.futureValue
      repository.currentEvent().futureValue shouldBe before
    }
  }

class EmptyRepositorySpec extends AnyWordSpec with Matchers with ScalaFutures with GuiceOneAppPerSuite:

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(Helpers.inMemoryDatabase()).build()

  private def repository = app.injector.instanceOf[VotingRepository]

  "A repository without events" should {
    "report that no event is open, whatever is asked" in {
      repository.currentEvent().failed.futureValue shouldBe NoOpenEvent
      repository.results().failed.futureValue shouldBe NoOpenEvent
      repository.snapshot().failed.futureValue shouldBe NoOpenEvent
      repository.castVote(Ballot("alice", 1, 1)).failed.futureValue shouldBe NoOpenEvent
    }
  }
