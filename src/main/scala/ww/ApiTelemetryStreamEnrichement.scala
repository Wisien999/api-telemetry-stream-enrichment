package ww

import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.io.kafka.KafkaIO
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.transforms.{MapElements, ParDo, ProcessFunction, SimpleFunction}
import org.apache.beam.sdk.values.{KV, TypeDescriptors}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}


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

    val kafkaWrite = KafkaIO.write[String, String]()
      .withBootstrapServers("localhost:9098")
      .withTopic("enriched-api-telemetry")
      .withValueSerializer(classOf[StringSerializer])
      .values()


    // Build the pipeline
    pipeline
      .apply("ReadFromKafka", kafkaRead)
      .apply(
        "ProcessRecords",
        MapElements.into(TypeDescriptors.strings()).via(new ProcessFunction[KV[String, ApiUsageEvent], String] {
            override def apply(input: KV[String, ApiUsageEvent]): String = {
              println(input.getValue)
              input.getValue.version.toString
            }
          })
      )
      .apply("RedisEnrichment", ParDo.of(new RedisEnrich))
      .apply("PrintToStdOut", MapElements.via(new SimpleFunction[String, String]() {
        override def apply(input: String): String = {
          println(input)
          input
        }
      }))
      .apply("PublishToKafka", kafkaWrite)

    // Run the pipeline
    pipeline.run().waitUntilFinish()
  }
}
