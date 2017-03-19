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
