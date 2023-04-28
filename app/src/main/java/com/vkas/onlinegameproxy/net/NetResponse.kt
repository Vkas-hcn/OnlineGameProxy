package com.vkas.onlinegameproxy.net

data class NetResponse(
    val code: Int,//响应码
    val data: Any?,//响应数据内容
    val message: String//响应数据的结果描述
)