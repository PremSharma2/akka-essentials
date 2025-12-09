package part2Actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}


/**
 * TODO: Implement
 * Distributed Word counting
 * requester  ---> Computational task---> (WCM)--> computation task --> one child of type WCW
 * requester  <--- Computational Result<--- (WCM)<-- computation Result <-- one child of type WCW (flow is Left to right)
 * Schemes for scheduling tasks to children is Round Robin
 * task1 -- childActor1
 * task2---- childActor2
 * ...
 * ...
 *
 * task10 --- childActor10
 *
 */
object ParentChildActorExercise {

   sealed trait MasterProtocol // messages supported by master

   sealed trait WorkerProtocol // messages supported by worker

   sealed trait UserProtocol // messages supported by Requester i.e its a Response Message

  // These are the master messages
   case class Initialize(nChildren: Int) extends MasterProtocol

  //command sent by master to worker
   case class WordCountTask(text: String, replyTo: ActorRef[UserProtocol]) extends MasterProtocol

  // Command sent back by worker to master
   case class WordCountReply(id: Int, count: Int) extends MasterProtocol

  // Worker Command
   case class WorkerTask(id: Int, task: String) extends WorkerProtocol

  // Requester message
   case class Response(count: Int) extends UserProtocol

  //Master Actor Behaviour
  object WordCounterMaster {
    def apply(): Behavior[MasterProtocol] =
      Behaviors
        .receive {
          (context, message) =>
            message match {
              case Initialize(nChildren) =>
                context.log.info(s"master Spinning up the $nChildren children Actors")
                val childrenRefs = for {
                  i <- 1 to nChildren
                } yield context.spawn(WordCounterWorker(), s"Child-Actor-$i")
                active(childrenRefs, 0, 0, Map.empty)
              case _ => context.log.info(s"[Master]: Command  not Supported")
                Behaviors.same
            }

        }

    private def active(childRefs: Seq[ActorRef[WorkerProtocol]],
                       currentChildIndex: Int,
                       currentTaskId: Int,
                       requestMap: Map[Int, ActorRef[UserProtocol]]): Behavior[MasterProtocol] = {
      Behaviors
        .receive {
          (context, message) =>
            message match {

              case WordCountTask(text, replyTo) =>
                context.log.info(
                  s"" +
                    s"[Master] Hi I have recived the message to process " +
                    s"$text i will send it to ChildWorker Actor " +
                    s"currentChildIndex is $currentChildIndex")
                //sending Command to Worker Actor
                val task = WorkerTask(currentTaskId, text)
                val childRef = childRefs(currentChildIndex)
                //send task to child
                childRef ! task
                // update my Data
                val nextChildIndex = (currentChildIndex + 1) % childRefs.length
                val nextTaskId = currentTaskId + 1
                val newRequestMap = requestMap.updated(nextTaskId,replyTo)
                active(childRefs, nextChildIndex, nextTaskId, newRequestMap)

              case WordCountReply(id, count) =>
                context.log.info(s"[Master]: I have received an Reply from the worker Actor for Task $id with $count")
                //fetching the Requester name
                val originalSender = requestMap(id)
                //Sending back the Response to Requester
                originalSender ! Response(count)
                active(childRefs, currentChildIndex, currentTaskId, requestMap - id)

              case _ => context.log.info(s"[Master]: Command  not Supported")
                Behaviors.same
            }
        }
    }
  }


  object WordCounterWorker {
    def apply(): Behavior[WorkerProtocol] = ???
  }

  //TODO Requester receiving Response from all Workers via WCM and handling response for calculating counts
  object Aggregator {
    def apply(): Behavior[UserProtocol] = active()

    def active(totalWords: Int = 0): Behavior[UserProtocol] = Behaviors.receive { (context, message) =>
      message match {
        case Response(count) =>
          context.log.info(s"[Requester Aggregator] received Response from WCM $count and total words are ${totalWords + count}")
          active(totalWords + count)

      }
    }
  }


  def testWordCounter(): Unit = {
    val userGuardianBehavior: Behavior[Unit] = Behaviors.setup { context =>
      // setup all the imp Buisness Actors of your application
      //setup the interaction between the Actors
      context.log.info(s"Bootstrapping the Actor Hierarchy Spinning up the Guardian Actor")
      val aggregator = context.spawn(Aggregator(), "Aggregator-Actor")
      val wcm = context.spawn(WordCounterMaster(), "WCM-Actor")
      // asking master create 3 Child Actors or workers
      wcm ! Initialize(3)
      // This is the task for worker and reply will sent back to Aggregator which is handling all response
      wcm ! WordCountTask("I Love Meerut", aggregator)
      wcm ! WordCountTask("Scala is super Dope", aggregator)
      wcm ! WordCountTask("Yes it is !!!", aggregator)
      // userGuardian Actor usually  has no  Behavior of its own
      Behaviors.empty
    }
    val actorSystem = ActorSystem(userGuardianBehavior, "WordCounting")
    Thread.sleep(1000)
    actorSystem.terminate()
  }

  def main(args: Array[String]): Unit = {
    testWordCounter()
  }
}
