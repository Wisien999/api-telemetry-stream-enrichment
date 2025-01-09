package ww

import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.DoFn.{ProcessElement, Setup, Teardown}
import org.apache.kafka.common.serialization.Serializer
import org.redisson.Redisson
import org.redisson.api.{RBucket, RedissonClient}
import org.redisson.codec.TypedJsonJacksonCodec
import org.redisson.config.Config

import java.util.UUID


case class EnrichedData(apiUsageEvent: ApiUsageEvent, user: Option[User])
object EnrichedData {
  val TypedJsonCodec = new TypedJsonJacksonCodec(classOf[EnrichedData], ScalaJacksonJsonMapper)

  class KafkaSerializer extends Serializer[EnrichedData] {
    override def serialize(topic: String, data: EnrichedData): Array[Byte] = {
      val buf = TypedJsonCodec.getValueEncoder.encode(data)
      val array = new Array[Byte](buf.readableBytes())

      buf.getBytes(buf.readerIndex(), array);

      array
    }
  }
}

// Enrich messages by looking up data from Redis
class RedisEnrich extends DoFn[ApiUsageEvent, EnrichedData] {
  private var redisClient: RedissonClient = null


  @Setup
  def setup(): Unit = {
    // Setup runs once per worker
    val config = new Config()
    config.useSingleServer.setAddress("redis://127.0.0.1:6385")
    redisClient = Redisson.create(config)
  }

  @ProcessElement
  def processElement(context: DoFn[ApiUsageEvent, EnrichedData]#ProcessContext): Unit = {
    val input = context.element()

    println(input.userId.toString)
    val userDataBucket = redisBucket(input.userId)
    val userData = userDataBucket.getOption

    context.output(EnrichedData(input, userData))
  }

  private def redisBucket(userId: UUID): RBucket[User] =
    redisClient.getBucket(s"user:{${userId}}", User.JsonCodec)

  @Teardown
  def teardown(): Unit = {
    // Teardown runs when the pipeline shuts down
    redisClient.shutdown()
  }
}