package com.vkas.onlinegameproxy.bean

data class OgVpnBean (
    var og_city: String? = null,
    var og_country: String? = null,
    var og_ip: String? = null,
    var og_method: String? = null,
    var og_port: Int? = null,
    var og_pwd: String? = null,
    var og_check: Boolean? = false,
    var og_best: Boolean? = false
)