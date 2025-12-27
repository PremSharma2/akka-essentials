package part2Actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

object ActorsIntro {

  //TODO: Behaviour
  private val simpleActorBehavior: Behavior[String] =
    Behaviors
      .receiveMessage {
        //TODO partial function
        (message: String) =>
          // do something with the message
          println(s"[simple actor] I have received: $message")

          // new behavior for the NEXT message
          Behaviors.same
      }


  private def demoSimpleActor(): Unit = {
    // part 2: instantiate or boot strapping the actor system
    val actorSystem = ActorSystem(simpleActorBehavior, "FirstActorSystem")
    val actorSystem1 = ActorSystem(SimpleActor(), "SecondActorSystem")
    val actorSystem3 = ActorSystem(SimpleActor_V2(), "SecondActorSystem")
    // part 3: communicate!
    actorSystem ! "I am learning Akka" // asynchronously send a message
    actorSystem1 ! "I am learning Akka" // asynchronously send a message
    actorSystem3 ! "I am learning Akka" // asynchronously send a message
    // ! = the "tell" method

    // part 4: gracefully shut down
    /*
    Terminates this actor system by running akka.actor.CoordinatedShutdown
    with reason akka.actor.CoordinatedShutdown.ActorSystemTerminateReason.
If akka.coordinated-shutdown.run-by-actor-system-terminate is configured to off
 it will not run CoordinatedShutdown, but the ActorSystem and its actors will still be terminated.
     */
    Thread.sleep(1000)
    actorSystem.terminate()
  }

  //TODO : Best way to design ActorBehavior "refactor"
  private object SimpleActor {
    def apply(): Behavior[String] =
      Behaviors
        .receiveMessage { (message: String) =>
          // do something with the message
          println(s"[simple actor] I have received: $message")

          // new behavior for the NEXT message
          Behaviors.same
        }
  }

  private object SimpleActor_V2 {
    def apply(): Behavior[String] =
      Behaviors
        .receive { (context, message) =>
          // TODO : -> context is a data structure (ActorContext) with access to a variety of APIs
          //TODO simple example: logging
          context.log.info(s"[simple actor] I have received: $message")
          Behaviors.same
        }
  }

  /*
Behaviour is Data structure
ActorCell (heap memory)
 ├── mailbox
 ├── context
 └── behavior ───────────────▶ BehaviorReceiveMessage(handler = message => ...)
val ctx = new ActorContext(...)
val inner = factory(ctx)     // <-- your setup block runs ONCE here
actor.behavior = inner       // <-- installs the real behavior
Dispatcher invokes
BehaviorReceiveMessage.onMessage(ctx, "hello")

lifecycle explained
context.spawn(SimpleActor_V3(), "simple")
Akka evaluates
SimpleActor_V3.apply()
that returns : BehaviorSetup(factory)
heap structure
ActorCell
 ├── mailbox (empty)
 ├── behavior ──▶ BehaviorSetup(factory)
 └── context (NOT created yet)

STEP 1 — Actor is started (bootstrap phase)

This is the critical phase the API text refers to.
Allocates the ActorContext
Initializes mailbox, dispatcher hooks, supervision
Sees that the behavior is a BehaviorSetup
Now finally
val ctx = new ActorContext(...)
val innerBehavior = factory(ctx)   // YOUR setup block runs HERE
actor.behavior = innerBehavior

After set up completes
Heap AFTER setup completes
Heap structure looks like this
ActorCell
 ├── mailbox
 ├── context ──▶ ActorContext
 └── behavior ──▶ BehaviorReceiveMessage(handler)
   */
  object SimpleActor_V3 {
    def apply(): Behavior[String] =
      Behaviors
        .setup {
          context =>
            // actor "private" data and methods, behaviors etc
            // YOUR CODE HERE

            // behavior used for the FIRST message
            Behaviors.receiveMessage {
              message =>
                context.log.info(s"[simple actor] I have received Event:-> $message")
                Behaviors.same
            }
        }
  }


  def main(args: Array[String]): Unit = {
    demoSimpleActor()
  }

}
