package op.asd.bean
import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@Keep
@JsonClass(generateAdapter = true)
class OnlineData (
    @Json(name = "rMz")
    var serverList: MutableList<OnlineServer>? = null,

    @Json(name = "QNjxy")
    var smartList: MutableList<OnlineServer>? = null
) : Serializable