package part2Actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import part2Actors.ChildActorIntro.Parent.defaultBehaviour

object ChildActorIntro {
  /*
    TODO
      // TODO Actors hierarchy is Tree like structure
      //TODO Actors can create other actors parent -> child actor -> grandChild
      // TODO root of actor hierarchy = Guardian Actor (created by the ActorSystem)
     Actor System creates
       - the top Level Guardian (Root Guardian) with children actors
         - system guardian (for Akka internal messages)
         - user Guardian (for Custom Actors)
            - All our Actors are Child to User Guardian Child Actor

 ActorSystem
└── Root Guardian
    ├── System Guardian        (Akka internal)
    └── User Guardian          (our custom actors)
        ├── ParentActor
        │   └── ChildActor
        │       └── GrandChildActor
        └── SomeOtherActor

+------------------+
|      Actor       |
|                  |
|  receive(msg)    <-- Command from other actors / user code
|                  |
|  receiveSignal   <-- Signal from Akka runtime
+------------------+


     */
  //Adts Modelling
  sealed trait Command

  case class CreateChild(name: String) extends Command

  case class TellChild(message: String) extends Command

  case object StopChild extends Command

  case object WatchChild extends Command

  object Parent {

    def defaultBehaviour(): Behavior[Command] =
      Behaviors
        .receive {
          (context, message) =>
            message match {
              case CreateChild(name) =>
                context.log.info(s"parent creating the Child with name $name")
                val childRef = context.spawn(Child(), name)
                active(childRef)

            }

        }

    def active(childRef: ActorRef[String]): Behavior[Command] =
      Behaviors
        .receive[Command] {
          (context, message) =>
            message match {
              case TellChild(message) =>
                context.log.info(s"parent sending to message to Child")
                childRef ! message //<- sending message to child Actor
                Behaviors.same // Behaviour of Parent Actor

              case StopChild =>
                context.log.info("[Parent-Actor]: stopping child")
                context.stop(childRef) // only works with CHILD actors
                defaultBehaviour() //resting to default behavior as  Child is killed

              case WatchChild =>
                context.log.info("[parent] watching child")
                context.watch(childRef) // can use any ActorRef
                Behaviors.same

              case _ =>
                context.log.info(s"message not supported")
                Behaviors.same
            }
        }
        .receiveSignal {
          case (context, Terminated(childRefWhichDied)) => handleTermination(context, childRefWhichDied)
        }

    private def handleTermination(context: ActorContext[_], childRefWhichDied: ActorRef[_]): Behavior[Command] = {
      context.log.info(s"[Parent-Actor] Child ${childRefWhichDied.path} was killed by something...")
      defaultBehaviour()
    }
  }


  object Child {
    def apply(): Behavior[String] =
      Behaviors
        .receive { (context, message) =>
          context.log.info(s"[Child] received message from Parent $message")
          Behaviors.same
        }
  }

  def parentChildDemo(): Unit = {
    val userGuardianBehavior: Behavior[Unit] =
      Behaviors
        .setup { context =>
          // setup all the imp Buisness Actors of your application
          //setup the interaction between the Actors
          val parent = context.spawn(defaultBehaviour(), "Parent-Actor")
          parent ! CreateChild("Child-Actor")
          parent ! TellChild("Hey-Kid-You-There")
          parent ! StopChild
          parent ! CreateChild("child2")
          parent ! TellChild("yo new kid, how are you?")
          // userGuardian Actor usually  has no  Behavior of its own
          Behaviors.empty
        }
    val actorSystem = ActorSystem(userGuardianBehavior, "Demo-Parent-Child")

    Thread.sleep(1000)
    actorSystem.terminate()
  }

  def main(args: Array[String]): Unit = {
    parentChildDemo()
  }
}
