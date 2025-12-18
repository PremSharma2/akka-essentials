package actorTest

import actorTest.BasicSpec.{BlackHoleActor, LabTestActor, SimpleActor}
import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("Basic-Spec-Actor-System"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A simple actor" should {
    "send back the same message" in {
      val simpleActor = system.actorOf(Props[SimpleActor])
      val inputCommand = "hello-akka"

      simpleActor ! inputCommand

      expectMsg(inputCommand)
    }
  }

  "A black-hole actor" should {
    "not send back any message" in {
      val blackHoleActor = system.actorOf(Props[BlackHoleActor])
      blackHoleActor ! "hello-akka"

      expectNoMessage(1.second)
    }
  }

  "A Lab-Test actor" should {
    val rnd = new Random(1) // deterministic seed
    val labTestActor = system.actorOf(LabTestActor.props(rnd))
    "turn the string to UpperCase and send back the same message" in {

      val inputCommand = "hello-akka"

      labTestActor ! inputCommand

      val response = expectMsgType[String]
      response shouldBe inputCommand.toUpperCase
    }

    "handle the case of InputCommand of greeting type" in {


      labTestActor ! "greeting"

      val response = expectMsgType[String]
      response should (be("hello") or be("hi"))
      // or: Set("hello","hi") should contain (response)
    }

    "reply with both scala and akka for favoriteTech InputCommand (any order) with simple API" in {

      labTestActor ! "favoriteTech"

      expectMsgAllOf("Scala", "Akka")
    }

    "reply with both scala and akka for favoriteTech InputCommand (any order)" in {


      labTestActor ! "favoriteTech"

      val responses = receiveN(2, 3.seconds).map(_.toString).toSet

      responses shouldBe Set("Scala", "Akka")
    }

    "reply with both scala and akka for favoriteTech InputCommand (any order) sing expectMsgPF" in {
      labTestActor ! "favoriteTech"

      expectMsgPF() {
        case "Scala" =>
        case "Akka" =>
      }

    }
  }
}


object BasicSpec {

  // Custom Actor behaviour
  class SimpleActor extends Actor {
    override def receive: Receive = {
      //sending back message to Parent actor
      case message => sender() ! message

    }
  }

  // Custom Actor behaviour
  class BlackHoleActor extends Actor {
    override def receive: Receive = {
      Actor.emptyBehavior
    }
  }

  // Custom Actor behaviour
  class LabTestActor(random: Random) extends Actor {
    override def receive: Receive = {
      case "greeting" =>
        if (random.nextBoolean()) sender() ! "hi" else sender() ! "hello"

      case "favoriteTech" =>
        sender() ! "Scala"
        sender() ! "Akka"

      case message: String =>
        sender() ! message.toUpperCase
    }
  }


  object LabTestActor {
    def props(random: Random): Props =
      Props(new LabTestActor(random))
  }
}