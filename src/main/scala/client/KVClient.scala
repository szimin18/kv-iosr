package client

import java.lang.System.exit

import scala.io.StdIn

object KVClient {
  def main(args: Array[String]): Unit = {
    val kvStore = new KVStore

    while (true) {
      try {
        print("> ")
        val options: Array[String] = StdIn.readLine().split(' ')
        options(0) match {
          case "get" =>
            val key = options(1)
            val value = kvStore.get(key)
            println(s"value for $key is $value")
          case "put" =>
            val key = options(1)
            val value = options(2)
            kvStore.put(key, value)
            println("ok")
          case "contains" =>
            val key = options(1)
            val contains = kvStore.contains(key)
            if (contains) {
              println("Store contains $key")
            } else {
              println("Store does not contain $key")
            }
          case "remove" =>
            val key = options(1)
            val removed = kvStore.remove(key)
            if (removed) {
              println("Entry was deleted from store")
            } else {
              println("Entry was not present in the store")
            }
          case "exit" => exit(0)
          case _ => usage()
        }
      } catch {
        case e: Throwable => usage()
      }
    }
  }

  def usage() {
    println("Usage:")
    println("get key - gets value by key, null if not present")
    println("put key value - puts value at given key, overrides existing")
    println("contains key - checks if contains given key")
    println("remove key - removes value of key, returns if the value was present")
  }
}
