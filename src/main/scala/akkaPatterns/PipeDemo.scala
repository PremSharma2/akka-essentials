package akkaPatterns

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import util.utils.ActorSystemEnhancements

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object PipeDemo {
  val executor = Executors.newFixedThreadPool(4)
  implicit val externalEC: ExecutionContext
  = ExecutionContext
    .fromExecutorService(executor) // for running the external service

  //Adts Command
  //interaction with External Services that returns Future
  trait PhoneCallProtocol

  case class FindAndCallPhoneNumber(name: String) extends PhoneCallProtocol

  case class InitiatePhoneCall(number: Int) extends PhoneCallProtocol

  case class LogPhoneCallFailure(reason: Throwable) extends PhoneCallProtocol

  //Postgress-DB
  val db: Map[String, Int] = Map(
    "Prem" -> 123,
    "Mayank" -> 456,
    "Bakey" -> 999
  )

  //externa

  def callExternalService(name: String): Future[Int] = {
    // select phoneNo from people where ...
    Future(db(name))
  }

  //Actor
  object PhoneCallActor {
    def apply(): Behavior[PhoneCallProtocol] = Behaviors.receive {
      (context, command) =>
        command match {
          case FindAndCallPhoneNumber(name) =>
            context.log.info(s"Fetching the phone number for $name")
            // pipe pattern
            // 1 - have the Future ready form Some External Service
            val phoneNumberFuture = callExternalService(name)
            // 2 - pipe the Future result back to me as a message
            context.pipeToSelf(phoneNumberFuture) {
              case Success(number) => InitiatePhoneCall(number)
              case Failure(ex) => LogPhoneCallFailure(ex)
            }
            Behaviors.same

          case InitiatePhoneCall(number) =>
            // perform the phone call
            context.log.info(s"Initiating phone call to $number")
            Behaviors.same


          case LogPhoneCallFailure(reason) =>
            context.log.warn(s"Initiating phone call failed: $reason")
            Behaviors.same
        }

    }
  }

  def main(args: Array[String]): Unit = {

    val userGuardian = Behaviors.setup[Unit]
      { context =>
      val phoneCallActor = context.spawn(PhoneCallActor(), "phoneCallActor")

      phoneCallActor ! FindAndCallPhoneNumber("Bakey")

      Behaviors.empty
    }

    ActorSystem(userGuardian, "DemoPipePattern").withFiniteLifespan(2.seconds)
  }
}
