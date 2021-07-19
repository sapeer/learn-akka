package akkabasics

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorsIntro extends App {

  //STEP 1 - create actor system
  val actorSystem = ActorSystem("FirstActorSystem")
  println(actorSystem.name)

  //STEP 2 - create actors
  class WordCountActor extends Actor {
    //local actor data
    var totalWords = 0

    override def receive: PartialFunction[Any, Unit] = {
      case message: String =>
        println(s"[word counter] I have received : ${message}")
        totalWords += message.split(" ").length
      case anythingElse => println(s"[word counter] Unable to process ${anythingElse.toString}")
    }
  }

  //STEP 3 - instantiate actor
  val wordCounter: ActorRef = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  val anotherWordCounter: ActorRef = actorSystem.actorOf(Props[WordCountActor], "anotherWordCounter")

  //STEP 4 - send messages to actor via actorRef
  wordCounter ! "Hello, welcome to the world for Akka!"
  anotherWordCounter ! "Another message to Akka!"
}
