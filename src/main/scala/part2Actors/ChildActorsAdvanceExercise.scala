package part2Actors

object ChildActorsAdvanceExercise {
/**
 * TODO: Implement
     Distributed Word counting
     requester  ---> Computational task---> (WCM)--> computation task --> one child of type WCW
     requester  <--- Computational Result<--- (WCM)<-- computation Result <-- one child of type WCW (flow is Left to right)
     Schemes for scheduling tasks to children is Round Robin
     task1 -- childActor1
     task2---- childActor2
     ...
     ...

     task10 --- childActor10

 */

trait MasterProtocol // messages supported by master
trait WorkerProtocol // messages supported by worker
trait UserProtocol  // messages supported by Requester

  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }
}
