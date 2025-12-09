package part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2Actors.ChildActors.CreditCard.{AttachToAccount, CheckStatus}

object ChildActors {

  // Actors can create other actors
//Adts
  private object EventHandlerActor {

  sealed  trait Command
    case class CreateChild(name: String) extends Command
    case class TellChild(message: String) extends Command

  }



  //Todo Define Your Actor
  private class EventHandlerActor extends Actor {
    import EventHandlerActor._

    //  type Receive = PartialFunction[Any, Unit]
    override def receive: Receive = {
      case CreateChild(name) =>
       // context.log.info(s"parent creating the Child with name $name")
        println(s"${self.path} creating child")
        // create a new actor right HERE
        val childRef = context.actorOf(Props[Child], name)
        /*
        Changes the Actor's behavior to become the new 'Receive' (PartialFunction[Any, Unit]) handler.
        Replaces the current behavior on the top of the behavior stack.
         */
        context.become(withChild(childRef))
    }

    //  type Receive = PartialFunction[Any, Unit]
   //2nd Behaviour
    private def withChild(childRef: ActorRef): Receive = {
      //emitting and intermediate Event for another Actor
      case TellChild(message) => childRef forward message
    }
  }

  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} I got: $message")
    }
  }

  import EventHandlerActor._

  val system = ActorSystem("ParentChildDemo")
  val parent = system.actorOf(Props[EventHandlerActor], "parent")
  parent ! CreateChild("child")
  parent ! TellChild("hey Kid!")

  // actor hierarchies
  // parent -> child -> grandChild
  //        -> child2 ->

  /*
    Guardian actors (top-level)
    - /system = system guardian
    - /user = user-level guardian
    - / = the root guardian
   */

  /**
   * Actor selection
   */
  val childSelection = system.actorSelection("/user/parent/child2")
  childSelection ! "I found you!"

  /**
   * Danger!
   *
   * NEVER PASS MUTABLE ACTOR STATE, OR THE `THIS` REFERENCE, TO CHILD ACTORS.
   *
   * NEVER IN YOUR LIFE.
   */


  object NaiveBankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object InitializeAccount
  }


  class NaiveBankAccount extends Actor {
    import CreditCard._
    import NaiveBankAccount._

    var amount = 0

    override def receive: Receive = {
      case InitializeAccount =>
        val creditCardRef = context.actorOf(Props[CreditCard], "card")
        //emitted event for another Actor
        creditCardRef ! AttachToAccount(this) // !

      case Deposit(funds) => deposit(funds)
      case Withdraw(funds) => withdraw(funds)

    }

    def deposit(funds: Int) = {
      println(s"${self.path} depositing $funds on top of $amount")
      amount += funds
    }
    def withdraw(funds: Int) = {
      println(s"${self.path} withdrawing $funds from $amount")
      amount -= funds
    }
  }

  object CreditCard {
    case class AttachToAccount(bankAccount: NaiveBankAccount) // !!
    case object CheckStatus
  }


  private class CreditCard extends Actor {
    override def receive: Receive = {
      case AttachToAccount(account) =>
        context.become(attachedTo(account))
    }

    //behaviour 2
    private def attachedTo(account: NaiveBankAccount): Receive = {
      case CheckStatus =>
        println(s"${self.path} your messasge has been processed.")
        // benign
        account.withdraw(1) // because I can
    }
  }

  import CreditCard._
  import NaiveBankAccount._

  val bankAccountRef = system.actorOf(Props[NaiveBankAccount], "account")
  bankAccountRef ! InitializeAccount
  bankAccountRef ! Deposit(100)

  Thread.sleep(500)
  val ccSelection = system.actorSelection("/user/account/card")
  ccSelection ! CheckStatus

  // WRONG!!!!!!

}
