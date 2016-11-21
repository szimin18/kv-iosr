package server

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.{Cluster, Member}
import akka.cluster.ClusterEvent._
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Send, Subscribe}

sealed trait KeyValueOperation

case class Get(k: String) extends KeyValueOperation

case class Put(k: String, v: String) extends KeyValueOperation

case class Contains(k: String) extends KeyValueOperation

case class Delete(k: String) extends KeyValueOperation

case class Update(op: KeyValueOperation)

case object InitRequest

case class InitData(data: scala.collection.mutable.Map[String, String])

case object Test

class KVNode extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  val mediator = DistributedPubSub(context.system).mediator
  val topic = "distributedKVTopic"
  mediator ! Subscribe(topic, self)
  var store = scala.collection.mutable.Map.empty[String, String]

  var initialized = false

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case InitData(data) =>
      if (store.isEmpty) {
        this.store = data
        this.initialized = true
      }
    case Test => log.info("test works!")
    case op: KeyValueOperation =>
      sender ! performKVOperation(op)
      mediator ! Publish(topic, Update(op))
    case Update(op) =>
      if (sender != self) performKVOperation(op)
    case MemberUp(member) =>
      mediator ! Publish(topic, InitData(store.clone()))
      log.info("Member is Up: {}", member.address.toString)
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)
    case _: MemberEvent => // ignore
  }
  
  def performKVOperation(keyValueOperation: KeyValueOperation): String = {
    keyValueOperation match {
      case Get(k) =>
        store.get(k).map(v =>
          s"value for $k is $v").getOrElse(s"no value for $k")
      case Put(k, v) =>
        store.put(k, v).map(oldV =>
          s"updated from $oldV to $v").getOrElse(s"ok")
      case Contains(k) =>
        store.contains(k).toString
      case Delete(k) =>
        store.remove(k).map(v =>
          s"removed $k").getOrElse(s"key $k not present in map")
    }
  }
}
