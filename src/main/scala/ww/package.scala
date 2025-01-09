import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{DeserializationContext, DeserializationFeature, SerializerProvider}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.redisson.api.RBucket

package object ww {
  implicit class RBucketExt[V](bucket: RBucket[V]) {
    def getOption: Option[V] = Option(bucket.get())
  }

  private object EnchancerModule extends SimpleModule("EnchancerModule") {
    addSerializer(classOf[HttpMethod], (value: HttpMethod, gen: JsonGenerator, serializers: SerializerProvider) => gen.writeString(value.toString))
    addDeserializer(classOf[HttpMethod], (p: JsonParser, ctxt: DeserializationContext) => HttpMethod.byName(p.getText))

    addSerializer(classOf[Role], (value: Role, gen: JsonGenerator, serializers: SerializerProvider) => gen.writeString(value.toString))
    addDeserializer(classOf[Role], (p: JsonParser, ctxt: DeserializationContext) => Role.byName(p.getText))
  }


  final val ScalaJacksonJsonMapper = JsonMapper
    .builder()
    .addModule(DefaultScalaModule)
    .addModule(EnchancerModule)
    .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
    .build()

}
