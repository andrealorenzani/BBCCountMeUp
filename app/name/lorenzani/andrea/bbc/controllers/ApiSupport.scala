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

package name.lorenzani.andrea.bbc.controllers

import name.lorenzani.andrea.bbc.model.VotingError
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{BaseController, Result}

/** Shared translation of failures into JSON error responses: `{"error": "..."}`. */
trait ApiSupport:
  self: BaseController =>

  protected def errorBody(message: String): JsObject = Json.obj("error" -> message)

  protected def badRequest(messages: List[String]): Result =
    BadRequest(Json.obj("error" -> messages.mkString("; "), "errors" -> messages))

  protected val handleVotingErrors: PartialFunction[Throwable, Result] =
    case error @ (VotingError.NoOpenEvent | _: VotingError.UnknownCandidate) => NotFound(errorBody(error.getMessage))
    case error @ (_: VotingError.StaleEvent | _: VotingError.VoteLimitReached) => Conflict(errorBody(error.getMessage))
