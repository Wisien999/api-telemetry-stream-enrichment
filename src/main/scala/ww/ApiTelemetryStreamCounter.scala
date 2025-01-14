package ww

import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.io.kafka.KafkaIO
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.transforms.DoFn.ProcessElement
import org.apache.beam.sdk.transforms.windowing.{FixedWindows, Window}
import org.apache.beam.sdk.transforms._
import org.apache.beam.sdk.values.KV
import org.apache.kafka.common.serialization.StringDeserializer
import org.joda.time.Duration

object ApiTelemetryStreamCounter {
  def main(cmdlineArgs: Array[String]): Unit = {
    val options = PipelineOptionsFactory.create()

    // Create the pipeline
    val pipeline = Pipeline.create(options)

    // Read from Kafka
    val kafkaRead = KafkaIO.read[String, EnrichedData]()
      .withBootstrapServers("localhost:9098")
      .withTopic("enriched-api-telemetry")
      .withKeyDeserializer(classOf[StringDeserializer])
      .withValueDeserializer(classOf[EnrichedData.KafkaDeserializer])
      .withoutMetadata()

    val k = new SerializableFunction[KV[String, Long], Boolean] {
      override def apply(input: KV[String, Long]): Boolean = true
    }

    val t = pipeline
      .apply("ReadFromKafka", kafkaRead)
      .apply("ExtractUserId", MapElements.via(new SimpleFunction[KV[String, EnrichedData], KV[String, EnrichedData]]() {
        override def apply(input: KV[String, EnrichedData]): KV[String, EnrichedData] = {
          KV.of(input.getValue.apiUsageEvent.userId.toString, input.getValue)
        }
      }))
      .apply("Window", Window.into[KV[String, EnrichedData]](FixedWindows.of(Duration.standardSeconds(30))))
      .apply("CountPerUser", Count.perKey[String, EnrichedData]())
      .apply("Filter", ParDo.of(new FilterCount))
      .apply("PrintToStdOut", MapElements.via(new SimpleFunction[KV[String, java.lang.Long], KV[String, java.lang.Long]]() {
        override def apply(input: KV[String, java.lang.Long]): KV[String, java.lang.Long] = {
          println(input)
          input
        }
      }))

    pipeline.run().waitUntilFinish()
  }

  class FilterCount extends DoFn[KV[String, java.lang.Long], KV[String, java.lang.Long]] {

    @ProcessElement
    def processElement(c: ProcessContext): Unit = {
      if (c.element().getValue > 5) {
        c.output(c.element())
      }
    }
  }

  class FilterRole extends DoFn[KV[String, EnrichedData], KV[String, EnrichedData]] {

    @ProcessElement
    def processElement(c: ProcessContext): Unit = {
      val user = c.element().getValue.user
      if (user.role == Role.User) {
        c.output(c.element())
      }
    }
  }
}
