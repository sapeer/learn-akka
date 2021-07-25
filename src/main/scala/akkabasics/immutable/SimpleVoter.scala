package akkabasics.immutable

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object SimpleVoter extends App {

  case class Vote(candidate: String)
  case object VoteStatusRequest
  case class VoteStatusReply(candidate: Option[String])

  class Citizen extends Actor {
    var candidate: Option[String] = None

    override def receive: Receive = {
      case Vote(c) => candidate = Some(c)
      case VoteStatusRequest => sender() ! VoteStatusReply(candidate)
    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])
  class VoteAggregator extends Actor {
    var stillWaiting: Set[ActorRef] = Set()
    var currentStatus: Map[String, Int] = Map()

    override def receive: Receive = {
      case AggregateVotes(citizens) =>
        stillWaiting = citizens
        citizens.foreach(citizenRef => citizenRef ! VoteStatusRequest)

        //possible infinite loop. Change to ask x times??
      case VoteStatusReply(None) => sender() ! VoteStatusRequest

      case VoteStatusReply(Some(c)) =>
        val newStillWaiting = stillWaiting - sender()
        val currentVotesOfCandidates = currentStatus.getOrElse(c, 0)
        currentStatus = currentStatus + (c -> (currentVotesOfCandidates + 1))
        if (newStillWaiting.isEmpty) {
          println(s"[vote aggregator] poll stats :: $currentStatus")
        } else {
          stillWaiting = newStillWaiting
        }
    }
  }

  val voterSystem = ActorSystem("VoterSystem")

  val alice = voterSystem.actorOf(Props[Citizen])
  val bob = voterSystem.actorOf(Props[Citizen])
  val charlie = voterSystem.actorOf(Props[Citizen])
  val daniel = voterSystem.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  val voteAggregator = voterSystem.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))

}
