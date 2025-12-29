package part2Actors

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.{Behaviors}
import akka.actor.typed.Terminated

object DemoSupervisionWithRestart {

  // --- Parent protocol ---
  sealed trait ParentCommand

  final case class Forward(text: String) extends ParentCommand

  private final case class ChildTerminated(name: String) extends ParentCommand

  // --- Child protocol ---
  sealed trait ChildCommand

  final case class CountWords(text: String) extends ChildCommand

  /**
   * Child actor.
   * will throw Exception.
   */
  private def fussyWordCounter(): Behavior[ChildCommand] =
    Behaviors.setup { ctx =>
      def running(totalWords: Int): Behavior[ChildCommand] =
        Behaviors.receiveMessage {
          case CountWords(text) =>
            // Example "fussy" failures
            if (text == null) throw new NullPointerException("null text")
            if (text.contains("Hide")) throw new RuntimeException("I don't like 'Hide'")
            val words = text.split("\\s+").count(_.nonEmpty)
            ctx.log.info(s"Counted $words words, new total = ${totalWords + words}")
            running(totalWords + words)
        }

      running(totalWords = 0)
    }

  /**
   * Supervision wrapper: make rules obvious and safe.
   *
   * NOTE: NullPointerException is a RuntimeException, so order matters.
   * We catch NPE first (resume), then other RuntimeExceptions (restart).
   */
  private def supervisedChild(): Behavior[ChildCommand] =
    Behaviors
      .supervise(
        Behaviors
          .supervise(fussyWordCounter())
          .onFailure[NullPointerException](SupervisorStrategy.resume)
      )
      .onFailure[RuntimeException](SupervisorStrategy.restart)

  /**
   * Parent actor: spawns child, watches it, forwards commands.
   */
  def parent(): Behavior[ParentCommand] =
    Behaviors
      .setup {
        ctx =>
          val child: ActorRef[ChildCommand] =
            ctx.spawn(supervisedChild(), "fussyChild")
          ctx.watch(child)
      //define behaviors
          Behaviors
            .receiveMessage[ParentCommand] {
              case Forward(text) =>
                child ! CountWords(text)
                Behaviors.same

              case ChildTerminated(name) =>
                ctx.log.warn(s"Child terminated: $name")
                Behaviors.same
            }
            .receiveSignal {
              case (_, Terminated(ref)) =>
                // Turn the signal into a normal message (keeps logic in one place)
                ctx.self ! ChildTerminated(ref.path.name)
                Behaviors.same
            }
      }

  /**
   * User guardian: boots parent, sends messages, then stops.
   */
  def userGuardian(): Behavior[Unit] =
    Behaviors
      .setup {
        ctx =>
          val parentRef = ctx.spawn(parent(), "fussyCounter")

          val msgs = List(
            "Starting to understand this Akka business...",
            "Quick! Hide!",
            "Are you there?",
            null, // triggers NPE → resume
            "What are you doing?",
            "Are you still there?"
          )

          msgs.foreach(m => parentRef ! Forward(m))
          Behaviors.empty
      }

  def main(args: Array[String]): Unit = {
    ActorSystem(userGuardian(), "DemoCrashWithParent")
  }
}
