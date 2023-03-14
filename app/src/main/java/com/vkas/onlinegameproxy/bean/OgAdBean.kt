package com.vkas.onlinegameproxy.bean

import androidx.annotation.Keep

@Keep
class OgAdBean (
    var ongpro_o_open: MutableList<OgDetailBean> = ArrayList(),
    var ongpro_n_home: MutableList<OgDetailBean> = ArrayList(),
    var ongpro_n_result: MutableList<OgDetailBean> = ArrayList(),
    var ongpro_i_2R: MutableList<OgDetailBean> = ArrayList(),
    var ongpro_i_2H: MutableList<OgDetailBean> = ArrayList(),
    var ongpro_n_ser: MutableList<OgDetailBean> = ArrayList(),

    var ongpro_cm: Int = 0,
    var ongpro_sm: Int = 0
)
@Keep
data class OgDetailBean(
    val ongpro_id: String,
    val ongpro_from: String,
    val ongpro_type: String,
    val ongpro_y: Int
)