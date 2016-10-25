package server

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.{Cluster, Member}
import akka.cluster.ClusterEvent._

class KVNode extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  var neighbours = List.empty[Member]

  // subscribe to cluster changes, re-subscribe when restart 
  override def preStart(): Unit = {
    //#subscribe
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
    //#subscribe
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
      neighbours = member :: neighbours
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
      neighbours = neighbours.filterNot(_ == member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)
      neighbours = neighbours.filterNot(_ == member)
    case _: MemberEvent => // ignore
  }
}