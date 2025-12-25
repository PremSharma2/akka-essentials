package actorTest

import akka.actor.testkit.typed.CapturedLogEvent
import akka.actor.testkit.typed.Effect.Spawned
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, ScalaTestWithActorTestKit, TestInbox}
import org.scalatest.wordspec.AnyWordSpecLike
import org.slf4j.event.Level
import part2Actors.ParentChildActorExercise._

class SynchronousTestingSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "A word counter master" should {
    "spawn a child upon reception of the initialize message" in {
      val master = {
        //BehaviorTestKit =
        // “simulate the actor in a single thread and record what it would have done in the real system”.
        BehaviorTestKit(WordCounterMaster())
      }
      master.run(Initialize(1)) //todo:  synchronous "sending" of the Command Initialize(1)  to WCM Actor

      // check that an effect was produced by sending the above Command to WCM actor
      val effect = master.expectEffectType[Spawned[WorkerProtocol]]
      // inspect the contents of those effects
      effect.childName should equal("worker1")
    }

    "send a task to a child" in {
      val master = BehaviorTestKit(WordCounterMaster())
      master.run(Initialize(1))

      // from the previous test - "consume" the event
      val effect = master.expectEffectType[Spawned[WorkerProtocol]]

      val mailbox = TestInbox[UserProtocol]() // the "requester"'s inbox
      // start processing
      master.run(WordCountTask("Akka testing is pretty powerful!", mailbox.ref))
      // mock the reply from the child
      master.run(WordCountReply(1, 5))
      // test that the requester got the right message
      mailbox.expectMessage(Response(5))
    }

    "log messages" in {
      val master = BehaviorTestKit(WordCounterMaster())
      master.run(Initialize(1))
      master.logEntries() shouldBe Seq(CapturedLogEvent(Level.INFO, "[master] initializing with 1 children"))
    }
  }
}
