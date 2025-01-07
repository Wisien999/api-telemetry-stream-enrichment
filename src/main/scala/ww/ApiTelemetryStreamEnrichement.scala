package ww

import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.transforms.{Create, MapElements, SimpleFunction}
import org.apache.beam.sdk.values.TypeDescriptors


object ApiTelemetryStreamEnrichement {
  def main(cmdlineArgs: Array[String]): Unit = {
    val options = PipelineOptionsFactory.create()

    // Create the pipeline
    val pipeline = Pipeline.create(options)

    // Create a simple pipeline
    pipeline
      .apply("CreateInput", Create.of("Hello", "Apache", "Beam"))
      .apply(
        "TransformToUpper",
        MapElements.into(TypeDescriptors.strings()).via((word: String) => word.toUpperCase)
      )
      .apply(
        "PrintElements",
        MapElements.via(new SimpleFunction[String, Void]() {
          override def apply(input: String): Void = {
            println(input)
            null
          }
        })
      )

    // Run the pipeline
    pipeline.run().waitUntilFinish()
  }
}
