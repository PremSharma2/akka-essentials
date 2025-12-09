package part2Actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}

object ChildActorsExercise {
  //Adts Modelling of Command
  sealed trait Command

  case class CreateChild(name: String) extends Command

  case class TellChild(nameOfActor: String, message: String) extends Command

  case class StopChild(name: String) extends Command

  case class WatchChild(name: String) extends Command

  object Parent_v2 {


    def defaultBehaviour(): Behavior[Command] = active(Map.empty)


    def active(children: Map[String, ActorRef[String]]): Behavior[Command] =
      Behaviors.receive[Command] {
          (context, event) =>
            event match {

              case CreateChild(name) =>
                context.log.info(s"[parent] creating child $name")
                val childref = context.spawn(Child(), name)
                active(children + (name -> childref))

              case StopChild(name) =>
                context.log.info(s"[Parent-Actor] attempting to stop child with name $name")
                val childOption = children.get(name)
                childOption.fold(context.log.info(
                  s"[Parent-Actor] Child $name " +
                    s"could not be stopped: name doesn't exist"))(context.stop)
                active(children - name)

              case WatchChild(name) =>
                context.log.info(s"[parent] watching child actor with the name $name")
                val childOption = children.get(name)
                childOption.fold(context.log.info(s"[parent] Cannot watch $name: name doesn't exist"))(context.watch)
                Behaviors.same

              case TellChild(name, message) =>
                val chidOption = children.get(name)
                chidOption.fold(context.log.info(s"[parent] Child $name Could not be Found"))(chidfRef => chidfRef ! message)
                Behaviors.same

            }
        }
        .receiveSignal {
          case (context, Terminated(childRefWhichDied)) =>
            handleTermination(context, childRefWhichDied)
            val childName = childRefWhichDied.path.name
            active(children - childName)
        }

    private def handleTermination(context: ActorContext[_], childRefWhichDied: ActorRef[_]): Behavior[Command] = {
      context.log.info(s"[Parent-Actor] Child ${childRefWhichDied.path} was killed by something...")
      defaultBehaviour()
    }
  }

  // Child Actor Definition
  object Child {
    def apply(): Behavior[String] =
      Behaviors
        .receive {
          (context, message) =>
          context.log.info(s"[${context.self.path.name}] received message from Parent-Actor $message")
          Behaviors.same
        }
  }

  def parentChildDemo(): Unit = {
    val userGuardianBehavior: Behavior[Unit] = Behaviors.setup {
      context =>
        // setup all the imp Buisness Actors of your application
        //setup the interaction between the Actors
        context.log.info(s"Bootstrapping the Actor Hierarchy Spinning up the Guardian Actor")
        val parent = context.spawn(Parent_v2.defaultBehaviour(), "Parent-Actor")
        parent ! CreateChild("Child-Actor")
        parent ! CreateChild("Child-Actor-1")
        parent ! WatchChild("Child-Actor-1")
        parent ! CreateChild("Child-Actor-2")
        parent ! TellChild("Child-Actor", "Hello-child")
        parent ! TellChild("Child-Actor-5", "Hello-child")
        parent ! StopChild("Child-Actor-1")
        parent ! TellChild("Child-Actor", "hey Alice, you still there?")
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