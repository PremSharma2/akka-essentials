package akkaPatterns

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.util.Timeout
import util.utils._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object AskDemoBetterDesign {

  // ----------------------------
  // 1) Protocol (your style: one ADT)
  // ----------------------------
  trait WorkProtocol

  // request to worker (includes replyTo, so can be used with ask)
  final case class ComputationalTask(payload: String, replyTo: ActorRef[WorkProtocol]) extends WorkProtocol

  // reply from worker
  final case class ComputationalResult(result: Int) extends WorkProtocol

  // orchestrator public command (external trigger)
  final case class StartWork(payload: String) extends WorkProtocol

  // orchestrator internal result wrapper (what orchestrator handles)
  final case class ExtendedComputationalResult(count: Int, description: String) extends WorkProtocol

  // ----------------------------
  // 2) Worker Actor
  // ----------------------------
  object Worker {
    def apply(): Behavior[WorkProtocol] =
      Behaviors.receive { (context, msg) =>
        msg match {
          case ComputationalTask(text, replyTo) =>
            context.log.info(s"[Worker-Actor]: ->  Crunching data for: $text")
            val count = text.split("\\s+").count(_.nonEmpty)
            replyTo ! ComputationalResult(count)
            Behaviors.same

          case _ => Behaviors.same
        }
      }
  }

  // ----------------------------
  // 3) OrchestratorActor Actor (does ASK, not guardian)
  // ----------------------------
  private object OrchestratorActor {

    def apply(worker: ActorRef[WorkProtocol])(implicit timeout: Timeout, scheduler: Scheduler): Behavior[WorkProtocol] =
      Behaviors.setup { context =>
        implicit val ec: ExecutionContext = context.executionContext

        Behaviors.receiveMessage {

          // external command kicks off ask
          case StartWork(payload) =>
            val replyF: Future[WorkProtocol] =
              worker.ask(replyTo => ComputationalTask(payload, replyTo)) // AskPattern.Askable enrichment

            // Convert Future completion into a message to *self* (keep actor model clean)
            //Akka will handle this message received form Temp Actor and send it to OrchestratorActor Actor mailbox
            context.pipeToSelf(replyF) {
              case Success(ComputationalResult(count)) =>
                ExtendedComputationalResult(count, "Ask completed successfully")
              case Success(other) =>
                ExtendedComputationalResult(-1, s"Unexpected reply: $other")
              case Failure(ex) =>
                ExtendedComputationalResult(-1, s"Computation failed: ${ex.getMessage}")
            }

            Behaviors.same

          // handle the "result message" inside the orchestrator mailbox
          case ExtendedComputationalResult(count, description) =>
            context.log.info(s"[Orchestrator-Actor:-> Received the Output Command From Akka] $description => count=$count")
            Behaviors.same

          case _ => Behaviors.same
        }
      }
  }

  // ----------------------------
  // 4) UserGuardian (bootstraps only)
  // ----------------------------
  private object UserGuardian {
    def apply(): Behavior[WorkProtocol] =
      Behaviors.
        setup[WorkProtocol] {
          context =>
            val worker = context.spawn(Worker(), "worker")

            // set up the implicits required by ask enrichment
            implicit val timeout: Timeout = Timeout(3.seconds)
            // NOTE: scheduler comes from ActorSystem; guardian has context.system.scheduler
            implicit val scheduler: Scheduler = context.system.scheduler

            val orchestrator = context.spawn(OrchestratorActor(worker), "orchestrator")

            // kick off a demo request (in real world: HTTP/Kafka/etc.)
            orchestrator ! StartWork("This ask pattern seems complicated but useful")

            Behaviors.empty
        }
  }

  def main(args: Array[String]): Unit = {
    ActorSystem(UserGuardian(), "AskBetterDesign").withFiniteLifespan(5.seconds)
  }
}
