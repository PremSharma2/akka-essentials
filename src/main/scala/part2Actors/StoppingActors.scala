package part2Actors

import akka.actor.typed.{ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors

object StoppingActors {

  object SensitiveActor {
    def apply(): Behavior[String] =
      Behaviors
        .receive[String] {
          (context, event) =>
            context.log.info(s"Received-External-Event->$event")
            if (event == "you're ugly")
              Behaviors.stopped(() => context.log.info(
                "Actor is being stopped  " +
                  "and Resource Release process started"
              )) // optionally pass a zero lambda() => Unit to clear up resources after the actor is stopped
            else
              Behaviors.same
        }
        .receiveSignal {
          case (context, PostStop) =>
            //clean up the resources
            context.log.info(
              s"PostStop Event is Received : -> $PostStop " +
                "and Resource Release process started")
            Behaviors.same
        }
  }

  def main(args: Array[String]): Unit = {
    //boot strapping up the Actor system with userGuardian actor
    val userGuardian = Behaviors.setup[Unit] {
      context =>
        val sensitiveActor = context.spawn(SensitiveActor(), "sensitiveActor")

        sensitiveActor ! "Hi"
        sensitiveActor ! "How are you"
        sensitiveActor ! "you're ugly"
        sensitiveActor ! "sorry about that"

        Behaviors.empty
    }

    val system = ActorSystem(userGuardian, "DemoStoppingActor")

    Thread.sleep(1000)
    system.terminate()

  }

}
