package com.vkas.onlinegameproxy.utils

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.gson.reflect.TypeToken
import com.vkas.onlinegameproxy.R
import com.vkas.onlinegameproxy.app.App.Companion.mmkvOg
import com.vkas.onlinegameproxy.bean.OgAdBean
import com.vkas.onlinegameproxy.bean.OgDetailBean
import com.vkas.onlinegameproxy.bean.OgVpnBean
import com.vkas.onlinegameproxy.bean.OpRemoteBean
import com.vkas.onlinegameproxy.key.Constant
import com.xuexiang.xui.utils.ResUtils.getString
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.resource.ResourceUtils
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.nio.charset.Charset

object OnlineGameUtils {
    private var installReferrer: String = ""
    fun getFastIpOg(): OgVpnBean {
        val ufVpnBean: MutableList<OgVpnBean> = getLocalServerData()
        val intersectionList =
            findFastAndOrdinaryIntersection(ufVpnBean).takeIf { it.isNotEmpty() } ?: ufVpnBean
        return intersectionList.shuffled().first().apply {
            og_best = true
            ongpro_country = getString(R.string.fast_service)
        }
    }

    /**
     * 获取本地服务器数据
     */
    fun getLocalServerData(): MutableList<OgVpnBean> {
        val listType = object : TypeToken<MutableList<OgVpnBean>>() {}.type

        return runCatching {
            JsonUtil.fromJson<MutableList<OgVpnBean>>(
                mmkvOg.decodeString(Constant.PROFILE_OG_DATA),
                listType
            )
        }.getOrNull() ?: JsonUtil.fromJson(
            ResourceUtils.readStringFromAssert(Constant.VPN_LOCAL_FILE_NAME_SKY),
            object : TypeToken<MutableList<OgVpnBean>?>() {}.type
        )
    }

    /**
     * 获取本地Fast服务器数据
     */
    private fun getLocalFastServerData(): MutableList<String> {
        val listType = object : TypeToken<MutableList<String>>() {}.type
        return runCatching {
            JsonUtil.fromJson<MutableList<String>>(
                mmkvOg.decodeString(Constant.PROFILE_OG_DATA_FAST),
                listType
            )
        }.getOrNull() ?: JsonUtil.fromJson(
            ResourceUtils.readStringFromAssert(Constant.FAST_LOCAL_FILE_NAME_SKY),
            object : TypeToken<MutableList<String>>() {}.type
        )
    }

    private fun findFastAndOrdinaryIntersection(ufVpnBeans: MutableList<OgVpnBean>): MutableList<OgVpnBean> {
        val intersectionList: MutableList<OgVpnBean> = mutableListOf()
        val fastServerData = getLocalFastServerData()
        intersectionList.addAll(ufVpnBeans.filter { fastServerData.contains(it.ongpro_city) })
        return intersectionList
    }

    /**
     * 广告排序
     */
    private fun adSortingOg(elAdBean: OgAdBean): OgAdBean {
        val adBean = OgAdBean()
        adBean.ongpro_o_open =
            sortByWeightDescending(elAdBean.ongpro_o_open) { it.ongpro_y }.toMutableList()
        adBean.ongpro_n_home =
            sortByWeightDescending(elAdBean.ongpro_n_home) { it.ongpro_y }.toMutableList()
        adBean.ongpro_n_result =
            sortByWeightDescending(elAdBean.ongpro_n_result) { it.ongpro_y }.toMutableList()
        adBean.ongpro_i_2R =
            sortByWeightDescending(elAdBean.ongpro_i_2R) { it.ongpro_y }.toMutableList()
        adBean.ongpro_i_2H =
            sortByWeightDescending(elAdBean.ongpro_i_2H) { it.ongpro_y }.toMutableList()
        adBean.ongpro_n_ser =
            sortByWeightDescending(elAdBean.ongpro_n_ser) { it.ongpro_y }.toMutableList()

        adBean.ongpro_sm = elAdBean.ongpro_sm
        adBean.ongpro_cm = elAdBean.ongpro_cm
        return adBean
    }

    /**
     * 根据权重降序排序并返回新的列表
     */
    private fun <T> sortByWeightDescending(list: List<T>, getWeight: (T) -> Int): List<T> {
        return list.sortedByDescending(getWeight)
    }

    /**
     * 取出排序后的广告ID
     */
    fun takeSortedAdIDOg(index: Int, elAdDetails: MutableList<OgDetailBean>): String {
        return elAdDetails.getOrNull(index)?.ongpro_id ?: ""
    }

    /**
     * 获取广告服务器数据
     */
    fun getAdServerDataOg(): OgAdBean {
        val serviceData: OgAdBean = runCatching {
            JsonUtil.fromJson(
                mmkvOg.decodeString(Constant.ADVERTISING_OG_DATA),
                OgAdBean::class.java
            )
        }.getOrNull() ?: JsonUtil.fromJson(
            ResourceUtils.readStringFromAssert(Constant.AD_LOCAL_FILE_NAME_SKY),
            object : TypeToken<OgAdBean>() {}.type
        )
        return adSortingOg(serviceData)
    }


    /**
     * 是否达到阀值
     */
    fun isThresholdReached(): Boolean {
        val clicksCount = mmkvOg.decodeInt(Constant.CLICKS_OG_COUNT, 0)
        val showCount = mmkvOg.decodeInt(Constant.SHOW_OG_COUNT, 0)
        if (clicksCount >= getAdServerDataOg().ongpro_cm || showCount >= getAdServerDataOg().ongpro_sm) {
            return true
        }
        return false
    }

    /**
     * 记录广告展示次数
     */
    fun recordNumberOfAdDisplaysOg() {
        var showCount = mmkvOg.decodeInt(Constant.SHOW_OG_COUNT, 0)
        showCount++
        MmkvUtils.set(Constant.SHOW_OG_COUNT, showCount)
    }

    /**
     * 记录广告点击次数
     */
    fun recordNumberOfAdClickOg() {
        var clicksCount = mmkvOg.decodeInt(Constant.CLICKS_OG_COUNT, 0)
        clicksCount++
        MmkvUtils.set(Constant.CLICKS_OG_COUNT, clicksCount)
    }

    /**
     * 通过国家获取国旗
     */
    fun getFlagThroughCountryEc(ec_country: String): Int {
        when (ec_country) {
            "Faster server" -> {
                return R.drawable.ic_fast
            }
            "Japan" -> {
                return R.drawable.ic_japan
            }
            "United Kingdom" -> {
                return R.drawable.ic_unitedkingdom
            }
            "United States" -> {
                return R.drawable.ic_usa
            }
            "Australia" -> {
                return R.drawable.ic_australia
            }
            "Belgium" -> {
                return R.drawable.ic_belgium
            }
            "Brazil" -> {
                return R.drawable.ic_brazil
            }
            "Canada" -> {
                return R.drawable.ic_canada
            }
            "France" -> {
                return R.drawable.ic_france
            }
            "Germany" -> {
                return R.drawable.ic_germany
            }
            "India" -> {
                return R.drawable.ic_india
            }
            "Ireland" -> {
                return R.drawable.ic_ireland
            }
            "Italy" -> {
                return R.drawable.ic_italy
            }
            "SouthKorea" -> {
                return R.drawable.ic_koreasouth
            }
            "Netherlands" -> {
                return R.drawable.ic_netherlands
            }
            "Newzealand" -> {
                return R.drawable.ic_newzealand
            }
            "Norway" -> {
                return R.drawable.ic_norway
            }
            "Russianfederation" -> {
                return R.drawable.ic_russianfederation
            }
            "Singapore" -> {
                return R.drawable.ic_singapore
            }
            "Sweden" -> {
                return R.drawable.ic_sweden
            }
            "Switzerland" -> {
                return R.drawable.ic_switzerland
            }
        }

        return R.drawable.ic_fast
    }

    fun getIpInformation() {
        val sb = StringBuffer()
        try {
            val url = URL("https://ip.seeip.org/geoip/")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            val code = conn.responseCode
            if (code == 200) {
                val `is` = conn.inputStream
                val b = ByteArray(1024)
                var len: Int
                while (`is`.read(b).also { len = it } != -1) {
                    sb.append(String(b, 0, len, Charset.forName("UTF-8")))
                }
                `is`.close()
                conn.disconnect()
                KLog.e("state", "sb==${sb.toString()}")
                MmkvUtils.set(Constant.IP_INFORMATION, sb.toString())
            } else {
                MmkvUtils.set(Constant.IP_INFORMATION, "")
                KLog.e("state", "code==${code.toString()}")
            }
        } catch (var1: Exception) {
            MmkvUtils.set(Constant.IP_INFORMATION, "")
            KLog.e("state", "Exception==${var1.message}")
            getIpInformation2()
        }
    }

    fun getIpInformation2() {
        val sb = StringBuffer()
        try {
            val url = URL("https://api.myip.com/")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            val code = conn.responseCode
            if (code == 200) {
                val `is` = conn.inputStream
                val b = ByteArray(1024)
                var len: Int
                while (`is`.read(b).also { len = it } != -1) {
                    sb.append(String(b, 0, len, Charset.forName("UTF-8")))
                }
                `is`.close()
                conn.disconnect()
                KLog.e("state", "sb2==${sb.toString()}")
                MmkvUtils.set(Constant.IP_INFORMATION2, sb.toString())
            } else {
                MmkvUtils.set(Constant.IP_INFORMATION2, "")
                KLog.e("state", "code2==${code.toString()}")
            }
        } catch (var1: Exception) {
            MmkvUtils.set(Constant.IP_INFORMATION2, "")
            KLog.e("state", "Exception2==${var1.message}")
        }
    }

    fun findFastestIP(ips: List<String>): String {
        var fastestIP = ""
        var fastestTime = Long.MAX_VALUE

        for (ip in ips) {
            try {
                val start = System.currentTimeMillis()
                val address = InetAddress.getByName(ip)
                val reachable = address.isReachable(1000)
                val end = System.currentTimeMillis()
                if (reachable && end - start < fastestTime) {
                    fastestIP = ip
                    fastestTime = end - start
                }
            } catch (e: Exception) {
                continue
            }
        }

        return fastestIP
    }

    fun referrer(
        context: Context,
    ) {
//        installReferrer = "gclid"
//        installReferrer = "fb4a"
//        MmkvUtils.set(Constant.INSTALL_REFERRER, installReferrer)
        try {
            val referrerClient = InstallReferrerClient.newBuilder(context).build()
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(p0: Int) {
                    when (p0) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            installReferrer =
                                referrerClient.installReferrer.installReferrer ?: ""
                            MmkvUtils.set(Constant.INSTALL_REFERRER, installReferrer)
                            KLog.e("TAG", "installReferrer====${installReferrer}")
                            referrerClient.endConnection()
                            return
                        }
                        else -> {
                            referrerClient.endConnection()
                        }
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                }
            })
        } catch (e: Exception) {
        }
    }

    fun isFacebookUser(): Boolean {
        val referrer = mmkvOg.decodeString(Constant.INSTALL_REFERRER) ?: ""
        return referrer.contains("fb4a", true)
                || referrer.contains("facebook", true)
    }

    fun isValuableUser(): Boolean {
        val referrer = mmkvOg.decodeString(Constant.INSTALL_REFERRER) ?: ""
        KLog.e("state", "referrer==${referrer}")
        return isFacebookUser()
                || referrer.contains("gclid", true)
                || referrer.contains("not%20set", true)
                || referrer.contains("youtubeads", true)
                || referrer.contains("%7B%22", true)
    }

    /**
     * 获取本地Vpn引导数据
     */
    fun getLocalVpnBootData(): OpRemoteBean {
        val listType = object : TypeToken<OpRemoteBean>() {}.type
        return runCatching {
            JsonUtil.fromJson<OpRemoteBean>(
                mmkvOg.decodeString(Constant.ONLINE_CONFIG),
                listType
            )
        }.getOrNull() ?: JsonUtil.fromJson(
            ResourceUtils.readStringFromAssert(Constant.VPN_BOOT_LOCAL_FILE_NAME_UF),
            object : TypeToken<OpRemoteBean?>() {}.type
        )
    }

    /**
     * 是否屏蔽插屏广告
     */
    fun whetherToBlockScreenAds(onlineRef: String): Boolean {
        when (onlineRef) {
            "1" -> {
                return true
            }
            "2" -> {
                return isValuableUser()
            }
            "3" -> {
                return isFacebookUser()
            }
            "4" -> {
                return false
            }
            else -> {
                return true
            }
        }
    }
}