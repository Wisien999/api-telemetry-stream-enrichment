package ww

import org.apache.kafka.common.serialization.Deserializer
import org.redisson.codec.TypedJsonJacksonCodec

import java.nio.charset.StandardCharsets
import java.util.UUID

case class ApiUsageEvent(apiVersion: String, userId: UUID, method: HttpMethod, path: String)
object ApiUsageEvent {
  object KafkaDeserializer extends Deserializer[ApiUsageEvent] {
    override def deserialize(topic: String, data: Array[Byte]): ApiUsageEvent = {

      val dataSeq = data.toSeq
      val version = topic

      val a: Array[Byte] = data.slice(0, 16) // UUID requires 16 bytes
      require(a.length == 16, "UUID requires exactly 16 bytes")

      val mostSigBits = a.slice(0, 8).foldLeft(0L)((sum, byte) => (sum << 8) | (byte & 0xFF))
      val leastSigBits = a.slice(8, 16).foldLeft(0L)((sum, byte) => (sum << 8) | (byte & 0xFF))

      val userId = new UUID(mostSigBits, leastSigBits)
      val methodNumber = dataSeq(16)
      val path = dataSeq.slice(17, dataSeq.length)

      ApiUsageEvent(
        version,
        userId,
        HttpMethod.byNumber(methodNumber),
        new String(path.toArray, StandardCharsets.UTF_8),
      )
    }
  }
}

sealed trait HttpMethod
object HttpMethod {
  case object Get extends HttpMethod
  case object Post extends HttpMethod
  case object Put extends HttpMethod
  case object Patch extends HttpMethod
  // I know there is more, idc

  val byNumber: Map[Byte, HttpMethod] = Map(
    1.toByte -> Get,
    2.toByte -> Post,
    3.toByte -> Put,
    4.toByte -> Patch,
  )

  val byName = List(Get, Post, Put, Patch).map(m => (m.toString, m)).toMap

}

case class User(userId: UUID, name: String, email: String, role: Role)
object User {
  val JsonCodec = new TypedJsonJacksonCodec(classOf[User], ScalaJacksonJsonMapper)
}

sealed trait Role
object Role {
  case object SuperUser extends Role
  case object Moderator extends Role
  case object User extends Role


  val byName = List(SuperUser, Moderator, User).map(r => (r.toString, r)).toMap
}
