package client

import java.lang.System.exit

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import com.typesafe.config.ConfigFactory
import server._

import scala.concurrent.{Await, Future}
import scala.io.StdIn
import scala.util.Random

object KVClient {
  def main(args: Array[String]): Unit = {

    val port = 2552 + new Random().nextInt(1000)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").withFallback(ConfigFactory.load())
    val system = ActorSystem("KVCluster", config)
    // Create an actor that handles cluster domain events
    implicit val kvNode = system.actorOf(Props[KVNode], name = s"kvNode$port")
    implicit val timeout = Timeout(2 seconds)

    while (true) {
      try {
        print("> ")
        val options: Array[String] = StdIn.readLine().split(' ')
        options(0) match {
          case "get" =>
            val key = options(1)
            println(askActor(Get(key)))
          case "put" =>
            val key = options(1)
            val value = options(2)
            println(askActor(Put(key, value)))
          case "contains" =>
            val key = options(1)
            println(askActor(Contains(key)))
          case "remove" =>
            val key = options(1)
            println(askActor(Delete(key)))
          case "exit" =>
            system.terminate()
            System.exit(0)
          case _ => usage()
        }
      } catch {
        case e: Throwable =>
          println(e)
          usage()
      }
    }
  }

  def askActor(msg: KeyValueOperation)(implicit actor: ActorRef, timeout: Timeout): String = {
    Await.result(actor ? msg, timeout.duration).asInstanceOf[String]
  }

  def usage() {
    println("Usage:")
    println("get key - gets value by key, null if not present")
    println("put key value - puts value at given key, overrides existing")
    println("contains key - checks if contains given key")
    println("remove key - removes value of key, returns if the value was present")
  }
}
