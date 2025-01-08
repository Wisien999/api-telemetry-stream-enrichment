package ww

import com.google.common.base.Utf8
import io.netty.buffer.{ByteBuf, Unpooled}
import org.apache.beam.sdk.options.ValueProvider.Deserializer
import org.redisson.codec.TypedJsonJacksonCodec

import java.io.InputStream
import java.nio.charset.StandardCharsets

case class ApiUsageEvent(version: Int, userId: Array[Byte], path: String)
//object ApiUsageEvent {
//  object KafkaDeserializer extends Deserializer {
//    override def deserialize(inputStream: InputStream): ApiUsageEvent = {
//      val version = inputStream.read()
//      val userId = inputStream.readNBytes(8)
//      val path = inputStream.readAllBytes()
//
//      ApiUsageEvent(version, userId, new String(path, StandardCharsets.UTF_8))
//    }
//  }
//}

case class User(userId: Int)
