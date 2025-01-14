package ww

import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.io.kafka.KafkaIO
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.transforms.{MapElements, ParDo, ProcessFunction, SimpleFunction}
import org.apache.beam.sdk.values.{KV, TypeDescriptor}
import org.apache.kafka.common.serialization.StringDeserializer

import scala.jdk.CollectionConverters._

object ApiTelemetryStreamEnrichement {
  def main(cmdlineArgs: Array[String]): Unit = {
    val options = PipelineOptionsFactory.create()

    // Create the pipeline
    val pipeline = Pipeline.create(options)

    // Read from Kafka
    val kafkaRead = KafkaIO.read[String, ApiUsageEvent]()
      .withBootstrapServers("localhost:9098")
      .withTopics(List("api-v1", "api-v2").asJava)
      .withKeyDeserializer(classOf[StringDeserializer])
      .withValueDeserializer(classOf[ApiUsageEvent.KafkaDeserializer.type])
      .withoutMetadata()

    val kafkaWrite = KafkaIO.write[String, EnrichedData]()
      .withBootstrapServers("localhost:9098")
      .withTopic("enriched-api-telemetry")
      .withValueSerializer(classOf[EnrichedData.KafkaSerializer])
      .values()

    // Build the pipeline
    val t = pipeline
      .apply("ReadFromKafka", kafkaRead)
      .apply("OrignalMessage", MapElements.via(new SimpleFunction[KV[String, ApiUsageEvent], KV[String, ApiUsageEvent]]() {
        override def apply(input: KV[String, ApiUsageEvent]): KV[String, ApiUsageEvent] = {
          println(s"Original message: ${input}")
          input
        }
      }))
      .apply("EnrichWithRedis", ParDo.of(new RedisEnrich))
//      .apply("PrintToStdOut", MapElements.via(new SimpleFunction[EnrichedData, EnrichedData]() {
//        override def apply(input: EnrichedData): EnrichedData = {
//          println(input)
//          input
//        }
//      }))
      .apply("PublishToKafka", kafkaWrite)

    // Run the pipeline
    pipeline.run().waitUntilFinish()
  }

}