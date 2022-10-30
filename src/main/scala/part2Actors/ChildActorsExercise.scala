package part2Actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import part2Actors.ChildActorsExercise.Parent_v2.{CreateChild, TellChild}

object ChildActorsExercise {

  object Parent_v2 {

    sealed trait Command

    case class CreateChild(name: String) extends Command

    case class TellChild(nameOfActor: String, message: String) extends Command

    def apply(): Behavior[Command] = active(Map.empty)

    def active(children: Map[String, ActorRef[String]]): Behavior[Command] = Behaviors.receive { (context, message) =>
      message match {
        case CreateChild(name) =>
          context.log.info(s"[parent] creating child $name")
          val childref = context.spawn(Child(), name)
          active(children + (name -> childref))
        case TellChild(name, message) =>
          val chidOption = children.get(name)
          chidOption.fold(context.log.info(s"[parent] Child $name Could not be Found"))(chidfRef => chidfRef ! message)
          Behaviors.same
      }
    }
  }

  // Child Actor Definition
  object Child {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      context.log.info(s"[${context.self.path.name}] received message from Parent $message")
      Behaviors.same
    }
  }

  def parentChildDemo() = {
    val userGuardianBehavior: Behavior[Unit] = Behaviors.setup { context =>
      // setup all the imp Buisness Actors of your application
      //setup the interaction between the Actors
      context.log.info(s"Bootstrapping the Actor Hierarchy Spinning up the Guardian Actor")
      val parent = context.spawn(Parent_v2(), "Parent-Actor")
      parent ! CreateChild("Child-Actor")
      parent ! CreateChild("Child-Actor-1")
      parent ! CreateChild("Child-Actor-2")
      parent ! TellChild("Child-Actor", "Hello-child")
      parent ! TellChild("Child-Actor-5", "Hello-child")
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