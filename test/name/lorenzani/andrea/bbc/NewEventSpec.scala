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

import name.lorenzani.andrea.bbc.model.{NewCandidate, NewEvent}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class NewEventSpec extends AnyFunSuite with Matchers:

  private val valid = NewEvent("Best song", "Vote!", List(NewCandidate("A"), NewCandidate("B", 5)))

  test("a well formed event is accepted") {
    NewEvent.validate(valid) shouldBe Right(valid)
  }

  test("a name and a description are required") {
    NewEvent.validate(valid.copy(name = " ", description = "")) shouldBe
      Left(List("The event needs a name", "The event needs a description"))
  }

  test("at least two candidates are required") {
    NewEvent.validate(valid.copy(candidates = List(NewCandidate("A")))).left.map(_.head) shouldBe
      Left("At least 2 candidates are required")
  }

  test("candidate names must be non blank and unique, ignoring surrounding spaces") {
    NewEvent.validate(valid.copy(candidates = List(NewCandidate("A"), NewCandidate(" A ")))) shouldBe
      Left(List("Candidate names must be unique"))
    NewEvent.validate(valid.copy(candidates = List(NewCandidate("A"), NewCandidate(" ")))) shouldBe
      Left(List("Candidate names cannot be blank"))
  }

  test("initial votes cannot be negative") {
    NewEvent.validate(valid.copy(candidates = List(NewCandidate("A", -1), NewCandidate("B")))) shouldBe
      Left(List("Initial votes cannot be negative"))
  }
