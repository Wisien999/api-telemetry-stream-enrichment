package ww

import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.io.kafka.KafkaIO
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.transforms.{MapElements, ProcessFunction}
import org.apache.beam.sdk.values.{KV, TypeDescriptor}
import org.apache.kafka.common.serialization.StringDeserializer
import ww.ApiUsageEvent.KafkaDeserializer

object ApiClient {

  def main(args: Array[String]): Unit = {
    val options = PipelineOptionsFactory.create()

    // Create the pipeline
    val pipeline = Pipeline.create(options)

    val kafkaRead = KafkaIO.read[String, ApiUsageEvent]()
      .withBootstrapServers("localhost:9098")
      .withTopic("enriched-api-telemetry")
      .withKeyDeserializer(classOf[StringDeserializer])
      .withValueDeserializer(classOf[KafkaDeserializer.type])
      .withoutMetadata()

    pipeline
      .apply("ReadFromKafka", kafkaRead)
      .apply(
        "LogRecords",
        MapElements.into(TypeDescriptor.of(classOf[ApiUsageEvent])).via(new ProcessFunction[KV[String, ApiUsageEvent], ApiUsageEvent] {
          override def apply(input: KV[String, ApiUsageEvent]): ApiUsageEvent = {
            println(f"Key: ${input.getKey}, Value: ${input.getValue}")
            input.getValue
          }
        })
      )

    pipeline.run().waitUntilFinish()
  }

}
