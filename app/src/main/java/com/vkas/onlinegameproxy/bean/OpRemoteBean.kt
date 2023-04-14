package com.vkas.onlinegameproxy.bean

import androidx.annotation.Keep

@Keep
data class OpRemoteBean(
    val online_start: String,
    val online_ratio: String,
    val online_ref:String
)
