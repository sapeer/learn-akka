package akkabasics

import akka.actor.{Actor, ActorSystem, Props}

object CounterActor extends App {
  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor {
    import Counter._

    var count = 0

    override def receive: Receive = {
      case Increment => count += 1
      case Decrement => count -= 1
      case Print => println(s"[counter actor] current counter is :: $count")
    }
  }

  val counterSystem = ActorSystem("CounterSystem")
  val counter = counterSystem.actorOf(Props[Counter], "myCounter")

  import Counter._
  (1 to 10).foreach(_ => counter ! Increment)
  (1 to 5).foreach(_ => counter ! Decrement)
  counter ! Print
}
