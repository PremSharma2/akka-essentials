import java.util.concurrent.Executors

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
object MultiThreadingRecap  extends App {

  val aThread= new Runnable {
    override def run(): Unit = println("JavaThread:->Executing")
  }
val aThread1= new Thread(() => println("ScalaThread:-> Executing"))

  aThread1.start()
  //todo: -problems with thread model is different runs produces different Result
  val aHellothread= new Thread(() => (1 to 100).foreach(_ => println("hello")))
  val aGoodByethread= new Thread(() => (1 to 100).foreach(_ => println("hello")))
aHellothread.start()
  aGoodByethread.start()

  // TODO : problem number2 with Existing java concurrency model is

  // DR #1: OO encapsulation is only valid in the SINGLE-THREADED MODEL

  class BankAccount(private var amount: Int) {
    override def toString = s"$amount"

    def withdraw(money: Int) = synchronized {
      this.amount -= money
    }

    def deposit(money: Int) = synchronized {
      this.amount += money
    }

    def getAmount = amount
  }

//interthread Communication on the JVM
  //wait - notify mechanism
//Scala Concurrency Model
  // Futures
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
  val aFuture = Future(/* something to be evaluated on another thread*/ 1 + 41)

  // register callback when it finishes
  aFuture.onComplete {
    case Success(value) => println(s"the async meaning of life is $value")
    case Failure(exception) => println(s"the meaning of value failed: $exception")
  }

  val aPartialFunction: PartialFunction[Try[Int], Unit] = {
    case Success(value) => println(s"the async meaning of life is $value")
    case Failure(exception) => println(s"the meaning of value failed: $exception")
  }

  aFuture.onComplete(aPartialFunction)
  //Interms of FP Future is monadic Construct
  // map, flatMap, filter, ...
  val doubledAsyncMOL: Future[Int] = aFuture.map(_ * 2)
  val filter: Future[Int] = aFuture.filter(_%2==0)
  val nonSenseFuture = for{
     a <- aFuture
     b <- filter
    if(a+b==2)
  }yield a+b


}
