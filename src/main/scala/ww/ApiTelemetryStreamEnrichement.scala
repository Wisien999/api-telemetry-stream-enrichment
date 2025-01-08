package ww

import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.coders.{Coder, CoderRegistry}
import org.apache.beam.sdk.io.kafka.{DeserializerProvider, KafkaIO, KafkaRecord}
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.transforms.{Create, MapElements, ParDo, ProcessFunction, SimpleFunction}
import org.apache.beam.sdk.values.{KV, TypeDescriptors}
import org.apache.kafka.common.serialization.{Deserializer, StringDeserializer}

import java.util


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
      .withValueDeserializer( new DeserializerProvider[ApiUsageEvent] {
        override def getDeserializer(configs: util.Map[String, _], isKey: Boolean): Deserializer[ApiUsageEvent] =
          ApiUsageEvent.KafkaDeserializer

        override def getCoder(coderRegistry: CoderRegistry): Coder[String] = ???
      })
      .withoutMetadata()


    // Build the pipeline
    val read = pipeline
      .apply("ReadFromKafka", kafkaRead)
      .apply(
        "ProcessRecords",
        MapElements.into(TypeDescriptors.strings()).via(new ProcessFunction[KV[String, String], String] {
            override def apply(input: KV[String, String]): String = {
              input.getValue
            }
          })
      )
      .apply("RedisEnrichment", ParDo.of(new RedisEnrich))
      .apply("PrintToStdOut", MapElements.via(new SimpleFunction[String, String]() {
        override def apply(input: String): String = {
          println(input)
          ""
        }
      }))

    // Run the pipeline
    pipeline.run().waitUntilFinish()
  }
}
