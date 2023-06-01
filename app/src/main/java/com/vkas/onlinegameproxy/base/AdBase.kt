package com.vkas.onlinegameproxy.base

import android.content.Context
import com.vkas.onlinegameproxy.ad.*
import com.vkas.onlinegameproxy.app.App
import com.vkas.onlinegameproxy.bean.OgAdBean
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.utils.KLogUtils
import com.vkas.onlinegameproxy.utils.OnlineGameUtils
import java.util.*

class AdBase {
    companion object {
        fun getOpenInstance() = InstanceHelper.openLoadOg
        fun getHomeInstance() = InstanceHelper.homeLoadOg
        fun getResultInstance() = InstanceHelper.resultLoadOg
        fun getConnectInstance() = InstanceHelper.connectLoadOg
        fun getBackInstance() = InstanceHelper.backLoadOg
        fun getListInstance() = InstanceHelper.listLoadOg
        private var idCounter = 0
    }

    val id = ++idCounter

    object InstanceHelper {
        val openLoadOg = AdBase()
        val homeLoadOg = AdBase()
        val resultLoadOg = AdBase()
        val connectLoadOg = AdBase()
        val backLoadOg = AdBase()
        val listLoadOg = AdBase()
    }

    var appAdDataOg: Any? = null

    // 是否正在加载中
    var isLoadingOg = false

    //加载时间
    var loadTimeOg: Long = Date().time

    // 是否展示
    var whetherToShowOg = false

    // openIndex
    var adIndexOg = 0

    // 是否是第一遍轮训
    var isFirstRotation: Boolean = false


    /**
     * 广告加载前判断
     */
    fun advertisementLoadingOg(context: Context) {
        val isAppOpenSameDay = App.isAppOpenSameDayOg()
        val isThresholdReached = OnlineGameUtils.isThresholdReached()
        val isAlreadyLoading = isLoadingOg
        val hasExpiredAd = appAdDataOg != null && !whetherAdExceedsOneHour(loadTimeOg)

        if (isThresholdReached) {
            KLogUtils.d( "广告达到上线")
            return
        }

        if (isAlreadyLoading) {
            KLogUtils.d( "${getInstanceName()}--广告加载中，不能再次加载")
            return
        }

        isFirstRotation = false

        if (appAdDataOg == null) {
            isLoadingOg = true
            KLogUtils.d( "${getInstanceName()}--广告开始加载")
            loadStartupPageAdvertisementOg(context, OnlineGameUtils.getAdServerDataOg())
        }

        if (hasExpiredAd) {
            isLoadingOg = true
            appAdDataOg = null
            KLogUtils.d("${getInstanceName()}--广告过期重新加载")
            loadStartupPageAdvertisementOg(context, OnlineGameUtils.getAdServerDataOg())
        }
    }

    /**
     * 广告是否超过过期（false:过期；true：未过期）
     */
    private fun whetherAdExceedsOneHour(loadTime: Long): Boolean =
        Date().time - loadTime < 60 * 60 * 1000


    /**
     * 加载启动页广告
     */
    private fun loadStartupPageAdvertisementOg(context: Context, adData: OgAdBean) {
        adLoaders[id]?.invoke(context, adData)
    }

    private val adLoaders = mapOf<Int, (Context, OgAdBean) -> Unit>(
        1 to ::loadOpenAdOg,
        2 to ::loadHomeAdOg,
        3 to ::loadResultAdOg,
        4 to ::loadConnectAdOg,
        5 to ::loadBackAdOg,
        6 to ::loadListAdOg
    )

    /**
     * 加载"open"类型广告
     */
    private fun loadOpenAdOg(context: Context, adData: OgAdBean) {
        val adType = adData.ongpro_o_open.getOrNull(adIndexOg)?.ongpro_type
        if (adType == "screen") {
            OgLoadOpenAd.loadStartInsertAdOg(context, adData)
        } else {
            OgLoadOpenAd.loadOpenAdvertisementOg(context, adData)
        }
    }

    /**
     * 加载"home"类型广告
     */
    private fun loadHomeAdOg(context: Context, adData: OgAdBean) {
        OgLoadHomeAd.loadHomeAdvertisementOg(context, adData)
    }

    /**
     * 加载"result"类型广告
     */
    private fun loadResultAdOg(context: Context, adData: OgAdBean) {
        OgLoadResultAd.loadResultAdvertisementOg(context, adData)
    }

    /**
     * 加载"connect"类型广告
     */
    private fun loadConnectAdOg(context: Context, adData: OgAdBean) {
        OgLoadConnectAd.loadConnectAdvertisementOg(context, adData)
    }

    /**
     * 加载"back"类型广告
     */
    private fun loadBackAdOg(context: Context, adData: OgAdBean) {
        OgLoadBackAd.loadBackAdvertisementOg(context, adData)
    }

    /**
     * 加载"list"类型广告
     */
    private fun loadListAdOg(context: Context, adData: OgAdBean) {
        OgLoadListAd.loadListAdvertisementOg(context, adData)
    }

    /**
     * 获取实例名称
     */
    private fun getInstanceName(): String {
        return when (id) {
            1 -> "open"
            2 -> "home"
            3 -> "result"
            4 -> "connect"
            5 -> "back"
            6 -> "list"
            else -> ""
        }
    }

}