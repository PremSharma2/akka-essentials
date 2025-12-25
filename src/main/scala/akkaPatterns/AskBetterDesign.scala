package akkaPatterns



import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object AskBetterDesign {

  // ---------- Worker protocol ----------
  private object Worker {
    sealed trait Command
    final case class ComputationalTask(payload: String, replyTo: ActorRef[Reply]) extends Command

    sealed trait Reply
    final case class ComputationalResult(result: Int) extends Reply

    def apply(): Behavior[Command] =
      Behaviors.receive { (context, msg) =>
        msg match {
          case ComputationalTask(text, replyTo) =>
            context.log.info(s"[worker] Crunching data for: $text")
            replyTo ! ComputationalResult(text.split("\\s+").count(_.nonEmpty))
            Behaviors.same
        }
      }
  }

  // ---------- Orchestrator protocol ----------
  private object Orchestrator {
    sealed trait Command
    final case class StartJob(payload: String) extends Command

    // internal messages (only orchestrator uses these)
    private sealed trait Internal extends Command
    private final case class JobResult(count: Int, description: String) extends Internal

    def apply(worker: ActorRef[Worker.Command]): Behavior[Command] =
      Behaviors.setup { context =>
        implicit val timeout: Timeout = 3.seconds

        Behaviors.receiveMessage {
          case StartJob(payload) =>
            // ASK: send request to worker, and when it completes,
            // map (Success/Failure) into a message to myself (JobResult)
            context.ask(worker, replyTo => Worker.ComputationalTask(payload, replyTo)) {
              case Success(Worker.ComputationalResult(count)) =>
                JobResult(count, s"Computed word-count for payload")
              case Failure(ex) =>
                JobResult(-1, s"Computation failed: ${ex.getMessage}")
            }
            Behaviors.same

          case JobResult(count, description) =>
            context.log.info(s"[orchestrator] $description => $count")
            Behaviors.same
        }
      }
  }

  // ---------- User Guardian (bootstrap only) ----------
  object UserGuardian {
    def apply(): Behavior[Unit] =
      Behaviors.setup { context =>
        val worker       = context.spawn(Worker(), "worker")
        val orchestrator = context.spawn(Orchestrator(worker), "orchestrator")

        // kick off a workflow
        orchestrator ! Orchestrator.StartJob("This ask pattern is much cleaner now")

        Behaviors.empty
      }
  }
}
