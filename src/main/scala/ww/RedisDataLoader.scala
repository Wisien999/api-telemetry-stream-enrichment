package ww

import org.redisson.Redisson
import org.redisson.api.{RBucket, RedissonClient}
import org.redisson.config.Config

import java.util.UUID
import scala.io.Source

object RedisDataLoader {

  private var redisClient: RedissonClient = null

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage: RedisDataLoader <file>")
      System.exit(1)
    }

    initConnection()
    val users = loadUsers(args(0))
    uploadUsers(users)
    redisClient.shutdown()
  }

  private def initConnection(): Unit = {
    val config = new Config()
    config.useSingleServer.setAddress("redis://127.0.0.1:6385")
    redisClient = Redisson.create(config)
  }

  private def loadUsers(filePath: String): List[User] = {
    println("Loading data")
    val source = Source.fromFile(filePath)
    val lines = source.getLines().drop(1) // Skip the header line
    val users = for (line <- lines) yield {
      val Array(uuid, name, email, role) = line.split(",")
      User(UUID.fromString(uuid), name, email, Role.byName(role))
    }

    val list = users.toList
    source.close()
    list
  }

  private def uploadUsers(users: List[User]): Unit = {
    println("Uploading data")
    for (user <- users) {
      val userDataBucket = redisBucket(user.userId)
      if (userDataBucket.getOption.isEmpty) {
        val newUser = User(user.userId, user.name, user.email, user.role)
        userDataBucket.set(newUser)
        println(s"Uploaded user ${user.userId}")
      }
      else {
        println(s"User ${user.userId} already exists")
      }
    }
  }

  private def redisBucket(userId: UUID): RBucket[User] =
    redisClient.getBucket(s"user:{${userId}}", User.JsonCodec)
}
