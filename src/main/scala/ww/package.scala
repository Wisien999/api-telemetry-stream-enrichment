import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.redisson.api.RBucket

package object ww {
  implicit class RBucketExt[V](bucket: RBucket[V]) {
    def getOption: Option[V] = Option(bucket.get())
  }


  final val ScalaJacksonJsonMapper = JsonMapper
    .builder()
    .addModule(DefaultScalaModule)
    .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
    .build()

}
