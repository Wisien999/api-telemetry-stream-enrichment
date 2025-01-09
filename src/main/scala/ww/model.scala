package ww

import org.apache.kafka.common.serialization.Deserializer

import java.nio.charset.StandardCharsets

case class ApiUsageEvent(version: Byte, userId: Seq[Byte], path: String)
object ApiUsageEvent {
  object KafkaDeserializer extends Deserializer[ApiUsageEvent] {
    override def deserialize(topic: String, data: Array[Byte]): ApiUsageEvent = {
      val dataSeq = data.toSeq
      val version = dataSeq.head
      val userId = dataSeq.slice(1, 9)
      val path = dataSeq.slice(9, dataSeq.length)

      ApiUsageEvent(version, userId, new String(path.toArray, StandardCharsets.UTF_8))
    }
  }
}

case class User(userId: Seq[Byte])
