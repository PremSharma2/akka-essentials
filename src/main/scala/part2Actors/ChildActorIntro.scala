package part2Actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import part2Actors.ChildActorIntro.Parent.{CreateChild, TellChild}

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


     */

  object Parent {

    def apply(): Behavior[Command] = Behaviors.receive { (context, message) =>
      message match {
        case CreateChild(name) =>
          context.log.info(s"parent creating the Child with name $name")
          val childRef = context.spawn(Child(), name)
          active(childRef)

      }

    }

    def active(childRef: ActorRef[String]): Behavior[Command] = Behaviors.receive { (context, message) =>
      message match {
        case TellChild(message) =>
          context.log.info(s"parent sending to message to Child")
          childRef ! message //<- sending message to child Actor
          Behaviors.same // Behaviour of Parent Actor
        case _ =>
          context.log.info(s"message not supported")
          Behaviors.same
      }
    }

    sealed trait Command

    case class CreateChild(name: String) extends Command

    case class TellChild(message: String) extends Command
  }

  object Child {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      context.log.info(s"[Child] received message from Parent $message")
      Behaviors.same
    }
  }

  def parentChildDemo() = {
    val userGuardianBehavior:Behavior[Unit] = Behaviors.setup{ context =>
      // setup all the imp Buisness Actors of your application
      //setup the interaction between the Actors
      val parent=context.spawn(Parent(),"Parent-Actor")
      parent ! CreateChild("Child-Actor")
      parent ! TellChild("Hey-Kid-You-There")
      // userGuardian Actor usually  has no  Behavior of its own
      Behaviors.empty
    }
    val actorSystem = ActorSystem(userGuardianBehavior,"Demo-Parent-Child")

    Thread.sleep(1000)
    actorSystem.terminate()
  }
  def main(args: Array[String]): Unit = {
     parentChildDemo()
  }
}
