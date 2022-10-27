package playground

import akka.actor.ActorSystem

object PlayGround extends App{
val actorSystem= ActorSystem("Hello-akka")
  println(actorSystem.name)
}
