package akkabasics

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hi !" =>
        sender() ! "Reply message !"

      case message: String => println(s"[simple actor :: ${context.self}] I have received $message")
      case number: Int => println(s"[simple actor] I have received a number $number")
      case specialMessage: SpecialMessage => println(s"[simple actor] I have received a special message ${specialMessage.contents}")
      case messageToSelf: MessageToSelf =>
        self ! messageToSelf.message

      case SayHiTo(ref) =>
        ref ! "Hi !"
    }
  }

  val actorSystem = ActorSystem("ActorDemo")
  val simpleActor = actorSystem.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "Hello Actor"
  simpleActor ! 42

  case class SpecialMessage(contents: String)
  case class MessageToSelf(message: String)

  simpleActor ! SpecialMessage("message from case class")
  simpleActor ! MessageToSelf("Akka is cool")

  val alice = actorSystem.actorOf(Props[SimpleActor], "alice")
  val bob = actorSystem.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)

  alice ! SayHiTo(bob)
}
