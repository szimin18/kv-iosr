package client

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

object KillerMonkey {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("clientSystem")
    val killerMonkey = system.actorOf(Props[KillerMonkey], "killerMonkey")
    killerMonkey ! KillRandomOne
  }
}

case object KillRandomOne

class KillerMonkey extends Actor with ActorLogging {
  override def receive: Receive = {
    case KillRandomOne =>
      context.actorSelection("akka.tcp://KVCluster@127.0.0.1:2551") ! PoisonPill
  }
}
