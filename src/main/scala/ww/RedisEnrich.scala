package ww

import io.netty.buffer.Unpooled
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.DoFn.{ProcessElement, Setup, Teardown}
import org.apache.beam.sdk.values.{KV, TupleTag}
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.redisson.Redisson
import org.redisson.api.{RBucket, RedissonClient}
import org.redisson.codec.TypedJsonJacksonCodec
import org.redisson.config.Config

import java.time.Instant
import java.util.{Date, UUID}


case class EnrichedData(apiUsageEvent: ApiUsageEvent, user: User, timestamp: java.util.Date) extends Serializable
object EnrichedData {
  val TypedJsonCodec = new TypedJsonJacksonCodec(classOf[EnrichedData], ScalaJacksonJsonMapper)

  class KafkaSerializer extends Serializer[EnrichedData] {
    override def serialize(topic: String, data: EnrichedData): Array[Byte] = {
      val buf = EnrichedData.TypedJsonCodec.getValueEncoder.encode(data)
      val array = new Array[Byte](buf.readableBytes())
      buf.getBytes(buf.readerIndex(), array)
      array
    }
  }

  class KafkaDeserializer extends Deserializer[EnrichedData] {

    override def deserialize(topic: String, data: Array[Byte]): EnrichedData = {
      val buf = Unpooled.wrappedBuffer(data)
      EnrichedData.TypedJsonCodec.getValueDecoder.decode(buf, null).asInstanceOf[EnrichedData]
    }
  }
}

// Enrich messages by looking up data from Redis
class RedisEnrich extends DoFn[KV[String, ApiUsageEvent], EnrichedData] {
  private var redisClient: RedissonClient = null

  val outputTag = new TupleTag[EnrichedData]()
  val noUser = new TupleTag[ApiUsageEvent]()
  val failuresTag = new TupleTag[String]()

  @Setup
  def setup(): Unit = {
    // Setup runs once per worker
    val config = new Config()
    config.useSingleServer.setAddress("redis://127.0.0.1:6385")
    redisClient = Redisson.create(config)
  }

  @ProcessElement
  def processElement(context: DoFn[KV[String, ApiUsageEvent], EnrichedData]#ProcessContext): Unit = {
    val input = context.element()

    val userDataBucket = redisBucket(input.getValue.userId)
    try {
      val userData = userDataBucket.getOption

      userData match {
        case Some(value) => context.output(EnrichedData(input.getValue, value, new Date()))
        case None => context.output(noUser, input.getValue)
      }
    } catch {
      case ex: Throwable =>
        context.output(failuresTag, ex.getMessage)
    }

  }

  private def redisBucket(userId: UUID): RBucket[User] =
    redisClient.getBucket(s"user:{${userId}}", User.JsonCodec)

  @Teardown
  def teardown(): Unit = {
    // Teardown runs when the pipeline shuts down
    redisClient.shutdown()
  }
}