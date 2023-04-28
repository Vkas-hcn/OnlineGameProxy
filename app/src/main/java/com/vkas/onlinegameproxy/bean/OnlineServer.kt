package com.vkas.onlinegameproxy.bean

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable
@Keep
@JsonClass(generateAdapter = true)
class OnlineServer (
    @Json(name = "Efnlbs")
    var mode: String?=null,//模式(协议)名

    @Json(name = "PliJPl")
    var ip: String?=null,//ip地址

    @Json(name = "EqE")
    var port: Int?=null,//端口号

    @Json(name = "UKOjZHJAUQ")
    var userd: String?=null,//用户名

    @Json(name = "jalicrdqmT")
    var password: String?=null,//密码

    @Json(name = "WnbzklQL")
    var method: String?=null,//加密方式

    @Json(name = "AgDV")
    var city: String?=null,//城市名

    @Json(name = "EBK")
    var country: String?=null,//国家名称

    @Json(name = "vLrCa")
    var countryCode: String?=null//国家码
) : Serializable
