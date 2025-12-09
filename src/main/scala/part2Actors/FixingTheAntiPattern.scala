package part2Actors



  import akka.actor.{Actor, ActorRef, ActorSystem, Props}

  // ======== PROTOCOL (messages) =========

  // high-level commands into the system these are input to platform
  private final case class AttachCard(cardId: String, accountId: String)
  private final case class DepositToAccount(accountId: String, amount: Int)
  private final case class CheckCardStatus(cardId: String)

  // internal wiring messages
  private final case class AttachToAccount(accountRef: ActorRef)

  // bank account protocol
  private final case class Deposit(amount: Int)
  private case object WithdrawOne
  private case object PrintBalance

  // credit card protocol
  private case object CheckStatus


  // ======== ACTORS =========

  // One actor per bank account (per accountId)
  private class NaiveBankAccount extends Actor {
    private var balance: Int = 0

    override def receive: Receive = {
      case Deposit(amount) =>
        balance += amount
        println(s"[${self.path}] deposited $amount, new balance = $balance")

      case WithdrawOne =>
        if (balance > 0) {
          balance -= 1
          println(s"[${self.path}] withdrew 1, new balance = $balance")
        } else {
          println(s"[${self.path}] insufficient funds to withdraw 1, balance = $balance")
        }

      case PrintBalance =>
        println(s"[${self.path}] current balance = $balance")
    }
  }

  // One actor per credit card (per cardId)
  // It gets "wired" to a particular NaiveBankAccount actor via AttachToAccount
  private class CreditCard extends Actor {

    override def receive: Receive = {
      case AttachToAccount(accountRef) =>
        println(s"[${self.path}] attached to account ${accountRef.path}")
        context.become(attachedTo(accountRef))
    }

    // After being attached, this behaviour is used
    private def attachedTo(accountRef: ActorRef): Receive = {
      case CheckStatus =>
        println(s"[${self.path}] your message has been processed.")
        // Send a message to the account actor (no direct method calls!)
        accountRef ! WithdrawOne
    }
  }


  // Manages accounts and cards:
  // - creates/fetches NaiveBankAccount actors per accountId
  // - creates CreditCard actors per cardId
  // - routes commands (like CheckCardStatus) to the right CreditCard
  private class AccountManager extends Actor {
    import context._

    // keep track of cardId -> card ActorRef
    private var cards: Map[String, ActorRef] = Map.empty

    override def receive: Receive = {

      case AttachCard(cardId, accountId) =>
        // 1. Get or create the account actor for this accountId
        val accountActor = childOrCreateAccount(accountId)

        // 2. Create a credit card actor for this cardId
        val cardActor = actorOf(Props[CreditCard](), s"card-$cardId")

        // 3. Remember this mapping
        cards += (cardId -> cardActor)

        // 4. Wire the card to that specific account actor
        cardActor ! AttachToAccount(accountActor)

      case DepositToAccount(accountId, amount) =>
        val accountActor = childOrCreateAccount(accountId)
        accountActor ! Deposit(amount)

      case CheckCardStatus(cardId) =>
        cards.get(cardId) match {
          case Some(cardRef) =>
            cardRef ! CheckStatus
          case None =>
            println(s"[${self.path}] No card with id '$cardId' found")
        }
    }

    // Either return existing account actor or create a new one for this accountId
    private def childOrCreateAccount(accountId: String): ActorRef =
      context.child(s"account-$accountId").getOrElse {
        val ref = context.actorOf(Props[NaiveBankAccount](), s"account-$accountId")
        println(s"[${self.path}] created account actor ${ref.path} for accountId=$accountId")
        ref
      }
  }


  // ======== MAIN APP =========

  object BankActorsDemo extends App {
    val system = ActorSystem("BankSystemDemo")

    val accountManager = system.actorOf(Props[AccountManager](), "accountManager")

    // --- set up some accounts and cards ---
    accountManager ! AttachCard("card-1", "acc-1")
    accountManager ! AttachCard("card-2", "acc-1") // second card for same account
    accountManager ! AttachCard("card-3", "acc-2") // card for a different account

    // deposit money into accounts
    accountManager ! DepositToAccount("acc-1", 5)
    accountManager ! DepositToAccount("acc-2", 2)

    Thread.sleep(500)

    // check status on different cards (each will withdraw 1 from its attached account)
    accountManager ! CheckCardStatus("card-1")
    accountManager ! CheckCardStatus("card-1")
    accountManager ! CheckCardStatus("card-2") // same acc-1, different card
    accountManager ! CheckCardStatus("card-3") // acc-2

    Thread.sleep(1000)

    // just to print final balances
    // (in a real design you might ask AccountManager to do this via messages)
    system.actorSelection("/user/accountManager/account-acc-1") ! PrintBalance
    system.actorSelection("/user/accountManager/account-acc-2") ! PrintBalance

    Thread.sleep(1000)
    system.terminate()
  }


