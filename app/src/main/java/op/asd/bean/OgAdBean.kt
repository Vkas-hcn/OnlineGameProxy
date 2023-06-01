package op.asd.bean

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class OgAdBean (
    var ongpro_o_open: MutableList<OgDetailBean> = ArrayList(),
    var ongpro_n_home: MutableList<OgDetailBean> = ArrayList(),
    var ongpro_n_result: MutableList<OgDetailBean> = ArrayList(),
    var ongpro_i_2R: MutableList<OgDetailBean> = ArrayList(),
    var ongpro_i_2H: MutableList<OgDetailBean> = ArrayList(),
    var ongpro_n_ser: MutableList<OgDetailBean> = ArrayList(),

    var ongpro_cm: Int = 0,
    var ongpro_sm: Int = 0
):Serializable
@Keep
data class OgDetailBean(
    val ongpro_id: String,
    val ongpro_from: String,
    val ongpro_type: String,
    val ongpro_y: Int,
    var ongpro_load_ip:String="",
    var ongpro_load_city:String="",
    var ongpro_show_ip:String="",
    var ongpro_show_city:String=""
)