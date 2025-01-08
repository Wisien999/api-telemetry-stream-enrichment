package ww

import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.DoFn.{ProcessElement, Setup, Teardown}
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config


// Enrich messages by looking up data from Redis
class RedisEnrich extends DoFn[String, String] {
  private var redisClient: RedissonClient = null


  @Setup
  def setup(): Unit = {
    // Setup runs once per worker
    val config = new Config()
    config.useSingleServer.setAddress("redis://127.0.0.1:6385")
    redisClient = Redisson.create(config)
  }

  @ProcessElement
  def processElement(context: DoFn[String, String]#ProcessContext): Unit = {
    val input = context.element()

    // Perform a lookup in Redis
    val enrichmentData = redisClient.getBucket[String](input).getOption
    val enrichedRecord = s"Input: $input, Enriched: $enrichmentData"
    println("inside enrichement stage: " + enrichmentData)

    // Output the enriched data
    context.output(enrichedRecord)
  }

  @Teardown
  def teardown(): Unit = {
    // Teardown runs when the pipeline shuts down
    redisClient.shutdown()
  }
}