package ww

import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.io.kafka.KafkaIO
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.transforms.{MapElements, ParDo, ProcessFunction, SimpleFunction}
import org.apache.beam.sdk.values.{KV, TypeDescriptor}
import org.apache.kafka.common.serialization.StringDeserializer


object ApiTelemetryStreamEnrichement {
  def main(cmdlineArgs: Array[String]): Unit = {
    val options = PipelineOptionsFactory.create()

    // Create the pipeline
    val pipeline = Pipeline.create(options)

    // Read from Kafka
    val kafkaRead = KafkaIO.read[String, ApiUsageEvent]()
      .withBootstrapServers("localhost:9098")
      .withTopic("api-v1")
      .withKeyDeserializer(classOf[StringDeserializer])
      .withValueDeserializer(classOf[ApiUsageEvent.KafkaDeserializer.type])
      .withoutMetadata()

    val kafkaWrite = KafkaIO.write[String, EnrichedData]()
      .withBootstrapServers("localhost:9098")
      .withTopic("enriched-api-telemetry")
      .withValueSerializer(classOf[EnrichedData.KafkaSerializer])
      .values()


    // Build the pipeline
    pipeline
      .apply("ReadFromKafka", kafkaRead)
      .apply(
        "ProcessRecords",
        MapElements.into(TypeDescriptor.of(classOf[ApiUsageEvent])).via(new ProcessFunction[KV[String, ApiUsageEvent], ApiUsageEvent] {
          override def apply(input: KV[String, ApiUsageEvent]): ApiUsageEvent = {
            println(input.getValue)
            input.getValue
          }
          })
      )
      .apply("RedisEnrichment", ParDo.of(new RedisEnrich))
      .apply("PrintToStdOut", MapElements.via(new SimpleFunction[EnrichedData, EnrichedData]() {
        override def apply(input: EnrichedData): EnrichedData = {
          println(input)
          input
        }
      }))
      .apply("PublishToKafka", kafkaWrite)

    // Run the pipeline
    pipeline.run().waitUntilFinish()
  }
}
