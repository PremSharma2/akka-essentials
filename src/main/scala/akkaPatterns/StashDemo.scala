package akkaPatterns

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import util.utils._
import scala.concurrent.duration._

object StashDemo {
  // an actor with locked access to a resource
  trait Command

  case object Open extends Command

  case object Close extends Command

  case object Read extends Command

  case class Write(data: String) extends Command

  /*
  TODO
   A Behavior is a data structure that describes how to handle messages.
   It is not executed immediately.
   It is installed into the actor.
   Behavior(
   stashBuffer = buffer,
   innerBehavior = closed("42", buffer)
   )
   A Behavior is a data structure (DS)
It is:
created once
stored in the actor instance
replaced when you return a new Behavior
invoked by Akka for each message
Actor instance
 ├── mailbox (queue)
 ├── currentBehavior : Behavior[T]   <-- pointer on heap
 ├── stashBuffer (optional, captured by behavior)
 └── ActorContext

def withStash[T](capacity: Int)(factory: StashBuffer[T] => Behavior[T]): Behavior[T] = {
  val buffer = new StashBuffer[T](capacity)
  val inner = factory(buffer)

  new Behavior[T] {
    def onMessage(ctx, msg): Behavior[T] = {
      inner.onMessage(ctx, msg)
    }
  }
}

ActorInstance
 ├── currentBehavior ──▶ Behavior_withStash
 │                       ├── stashBuffer ──▶ StashBuffer
 │                       └── innerBehavior ──▶ Behavior_initializing
 ├── mailbox
 └── ActorContext
Big Datastructure View
┌──────────────────────────────┐
│ ActorInstance                │
│                              │
│  mailbox ───────────────┐    │
│                           │    │
│  currentBehavior ─────┐  │    │
│                        │  │    │
│  Behavior               │  │    │
│   ├─ onMessage() ◄─────┘  │    │
│   ├─ stashBuffer          │    │
│   └─ closures/state       │    │
│                              │
└──────────────────────────────┘

   */
  object ResourceActor {
    def apply(): Behavior[Command] =
      Behaviors
        .withStash(128) { buffer =>
        closed("42", buffer)
      }

    private def closed(data: String, buffer: akka.actor.typed.scaladsl.StashBuffer[Command]): Behavior[Command] =
      Behaviors.receive { (context, msg) =>
        msg match {
          case Open =>
            context.log.info("Opening Resource")
            buffer.unstashAll(open(data, buffer))

          case other =>
            context.log.info(s"Stashing $other because resource is closed")
            buffer.stash(other)
            Behaviors.same
        }
      }

    private def open(data: String, buffer: akka.actor.typed.scaladsl.StashBuffer[Command]): Behavior[Command] =
      Behaviors.receive { (context, msg) =>
        msg match {
          case Read =>
            context.log.info(s"I have read $data")
            Behaviors.same

          case Write(newData) =>
            context.log.info(s"I have written $newData")
            open(newData, buffer)

          case Close =>
            context.log.info("Closing Resource")
            closed(data, buffer)

          case other =>
            context.log.info(s"$other not supported while open")
            Behaviors.same
        }
      }
  }


  def main(args: Array[String]): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      val resourceActor = context.spawn(ResourceActor(), "resource")

      resourceActor ! Read // stashed
      resourceActor ! Open // unstash the Read message after opening
      resourceActor ! Open // unhandled
      resourceActor ! Write("I love stash") // overwrite
      resourceActor ! Write("This is pretty cool") // overwrite
      resourceActor ! Read
      resourceActor ! Read
      resourceActor ! Close
      resourceActor ! Read // stashed: resource is closed

      Behaviors.empty
    }
    ActorSystem(userGuardian, "DemoStash").withFiniteLifespan(2.seconds)
  }

}
