package ww

import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.coders.{Coder, CoderRegistry, StringUtf8Coder}
import org.apache.beam.sdk.io.kafka.{DeserializerProvider, KafkaIO, KafkaRecord}
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.transforms.{Create, MapElements, ParDo, ProcessFunction, SimpleFunction}
import org.apache.beam.sdk.values.{KV, TypeDescriptors}
import org.apache.kafka.common.serialization.{Deserializer, StringDeserializer}

import java.io.{InputStream, OutputStream}
import java.util


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
      .withValueDeserializer( new DeserializerProvider[ApiUsageEvent] {
        override def getDeserializer(configs: util.Map[String, _], isKey: Boolean): Deserializer[ApiUsageEvent] = new Deserializer[ApiUsageEvent] {
          override def deserialize(topic: String, data: Array[Byte]): ApiUsageEvent = new ApiUsageEvent(1, new Array[Byte](1), "test")
        }

        override def getCoder(coderRegistry: CoderRegistry): Coder[ApiUsageEvent] = new Coder[ApiUsageEvent] {
          override def encode(value: ApiUsageEvent, outStream: OutputStream): Unit = {}

          override def decode(inStream: InputStream): ApiUsageEvent = new ApiUsageEvent(1, new Array[Byte](1), "test")

          override def getCoderArguments: util.List[_ <: Coder[_]] = util.Arrays.asList()

          override def verifyDeterministic(): Unit = {}
        }
      })
      .withoutMetadata()


    // Build the pipeline
    pipeline
      .apply("ReadFromKafka", kafkaRead)
      .apply(
        "ProcessRecords",
        MapElements.into(TypeDescriptors.strings()).via(new ProcessFunction[KV[String, ApiUsageEvent], String] {
            override def apply(input: KV[String, ApiUsageEvent]): String = {
              input.getValue.version.toString
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
