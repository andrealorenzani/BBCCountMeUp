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

import jakarta.inject.{Inject, Singleton}
import name.lorenzani.andrea.bbc.model.JsonFormats.given
import name.lorenzani.andrea.bbc.model.NewEvent
import name.lorenzani.andrea.bbc.repository.VotingRepository
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.*

import scala.concurrent.ExecutionContext

@Singleton
class AdminController @Inject() (val controllerComponents: ControllerComponents, repository: VotingRepository)(using
    ExecutionContext
) extends BaseController
    with ApiSupport:

  private val logger = Logger(getClass)

  def index: Action[AnyContent] = Action(Ok(views.html.admin()))

  def event: Action[AnyContent] = Action.async {
    repository.currentEvent().map(event => Ok(Json.toJson(event))).recover(handleVotingErrors)
  }

  def createEvent: Action[NewEvent] = Action.async(parse.json[NewEvent]) { request =>
    NewEvent.validate(request.body) match
      case Left(errors) => scala.concurrent.Future.successful(badRequest(errors))
      case Right(newEvent) =>
        repository.createEvent(newEvent).map { event =>
          logger.info(s"Opened event '${event.name}' with ${event.candidates.size} candidates")
          Created(Json.toJson(event))
        }
  }
