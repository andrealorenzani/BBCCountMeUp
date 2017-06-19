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

package name.lorenzani.andrea.bbc.dependencyinjection

import java.io.{File, FileInputStream}
import java.util.Properties

import akka.routing.RoundRobinPool
import com.google.inject.AbstractModule
import name.lorenzani.andrea.bbc.actors.{AdminActor, PresenterActor, VoterActor}
import play.api.libs.concurrent.AkkaGuiceSupport

class DIModule extends AbstractModule with AkkaGuiceSupport {
  def configure() = {
    val props = new Properties()
    props.load(new FileInputStream(new File("conf/application.conf")))
    val ninstancesvoter = props.getProperty("actors.router.voter.ninstances").toInt
    bindActor[VoterActor]("voter-actor", RoundRobinPool(ninstancesvoter).props)
    bindActor[PresenterActor]("presenter-actor")
    bindActor[AdminActor]("admin-actor")
  }
}
