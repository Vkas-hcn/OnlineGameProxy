package com.vkas.onlinegameproxy.bean

import androidx.annotation.Keep

@Keep
class OgAdBean (
    var og_open: MutableList<OgDetailBean> = ArrayList(),
    var og_vpn: MutableList<OgDetailBean> = ArrayList(),
    var og_result: MutableList<OgDetailBean> = ArrayList(),
    var og_connect: MutableList<OgDetailBean> = ArrayList(),
    var og_back: MutableList<OgDetailBean> = ArrayList(),
    var og_list: MutableList<OgDetailBean> = ArrayList(),

    var og_click_num: Int = 0,
    var og_show_num: Int = 0
)
@Keep
data class OgDetailBean(
    val og_id: String,
    val og_platform: String,
    val og_type: String,
    val og_weight: Int
)