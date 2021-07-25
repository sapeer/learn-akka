package akkabasics.immutable

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ImmutableVoter extends App {
    case class Vote(candidate: String)
    case object VoteStatusRequest
    case class VoteStatusReply(candidate: Option[String])

    class Citizen extends Actor {

      def voted(candidate: String): Receive = {
        case VoteStatusRequest => sender() ! VoteStatusReply(Some(candidate))
      }

      override def receive: Receive = {
        case Vote(c) => context.become(voted(c))
        case VoteStatusRequest => sender() ! VoteStatusReply(None)
      }
    }

    case class AggregateVotes(citizens: Set[ActorRef])
    class VoteAggregator extends Actor {
      override def receive: Receive = awaitingCommand

      def awaitingCommand: Receive = {
        case AggregateVotes(citizens) =>
          citizens.foreach(citizenRef => citizenRef ! VoteStatusRequest)
          context.become(awaitingStatuses(citizens, Map()))
      }

      def awaitingStatuses(stillWaiting: Set[ActorRef], currentStatus: Map[String, Int]): Receive = {
        //possible infinite loop. Change to ask x times??
        case VoteStatusReply(None) => sender() ! VoteStatusRequest

        case VoteStatusReply(Some(c)) =>
          val newStillWaiting = stillWaiting - sender()
          val currentVotesOfCandidates = currentStatus.getOrElse(c, 0)
          val newStatus = currentStatus + (c -> (currentVotesOfCandidates + 1))
          if (newStillWaiting.isEmpty) {
            println(s"[vote aggregator] poll stats :: $newStatus")
          } else {
            context.become(awaitingStatuses(newStillWaiting, newStatus))
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
