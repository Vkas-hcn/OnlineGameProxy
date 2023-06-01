package op.asd.bean

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import java.io.Serializable

@Keep
@JsonClass(generateAdapter = true)
class OnlineVpnBean (
    var code: String,
    var data: OnlineData,
    var msg: String
) : Serializable