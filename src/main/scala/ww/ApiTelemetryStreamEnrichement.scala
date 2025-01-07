package ww

import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.io.kafka.KafkaIO
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.transforms.{Create, MapElements, SimpleFunction}
import org.apache.beam.sdk.values.{KV, TypeDescriptors}
import org.apache.kafka.common.serialization.StringDeserializer


object ApiTelemetryStreamEnrichement {
  def main(cmdlineArgs: Array[String]): Unit = {
    val options = PipelineOptionsFactory.create()

    // Create the pipeline
    val pipeline = Pipeline.create(options)

    // Read from Kafka
    val kafkaRead = KafkaIO.read[String, String]()
      .withBootstrapServers("localhost:9098")
      .withTopic("api-v1")
      .withKeyDeserializer(classOf[StringDeserializer])
      .withValueDeserializer(classOf[StringDeserializer])
      .withoutMetadata()


    // Build the pipeline
    pipeline
      .apply("ReadFromKafka", kafkaRead)
      .apply(
        "ProcessRecords",
        MapElements.into(TypeDescriptors.kvs(TypeDescriptors.strings(), TypeDescriptors.strings()))
          .via((record: KV[String, String]) => {
            val key = Option(record.getKey).getOrElse("")
            val value = record.getValue.toUpperCase // Transform value to uppercase
            println("key:" + key + "   value: " + value)
            KV.of(key, value)
          }),
      )
      .apply("PrintToStdOut", MapElements.via(new SimpleFunction[KV[String, String], String]() {
        override def apply(input: KV[String, String]): String = {
          println(input)
          ""
        }
      }))

    // Run the pipeline
    pipeline.run().waitUntilFinish()
  }
}
