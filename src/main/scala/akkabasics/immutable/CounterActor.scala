package akkabasics.immutable

import akka.actor.{Actor, ActorSystem, Props}

object CounterActor extends App {
  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor {
    import Counter._

    override def receive: Receive = countReceive(0)

    def countReceive(counter: Int): Receive = {
      case Increment =>
        println(s"[counter] current count is $counter and inside Increment")
        context.become(countReceive(counter + 1))
      case Decrement =>
        println(s"[counter] current count is $counter and inside Decrement")
        context.become(countReceive(counter - 1))
      case Print => println(s"[counter] current count is :: $counter")
    }
  }

  val counterSystem = ActorSystem("CounterSystem")
  val counter = counterSystem.actorOf(Props[Counter], "myCounter")

  import Counter._
  (1 to 10).foreach(_ => counter ! Increment)
  counter ! Print
  (1 to 5).foreach(_ => counter ! Decrement)
  counter ! Print
}