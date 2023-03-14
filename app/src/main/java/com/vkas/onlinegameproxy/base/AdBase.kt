package com.vkas.onlinegameproxy.base

import android.content.Context
import com.vkas.onlinegameproxy.ad.*
import com.vkas.onlinegameproxy.app.App
import com.vkas.onlinegameproxy.bean.OgAdBean
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.utils.KLog
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
        App.isAppOpenSameDayOg()
        if (OnlineGameUtils.isThresholdReached()) {
            KLog.d(Constant.logTagOg, "广告达到上线")
            return
        }
        if (isLoadingOg) {
            KLog.d(Constant.logTagOg, "${getInstanceName()}--广告加载中，不能再次加载")
            return
        }
        isFirstRotation = false
        if (appAdDataOg == null) {
            isLoadingOg = true
            KLog.d(Constant.logTagOg, "${getInstanceName()}--广告开始加载")
            loadStartupPageAdvertisementOg(context, OnlineGameUtils.getAdServerDataOg())
        }
        if (appAdDataOg != null && !whetherAdExceedsOneHour(loadTimeOg)) {
            isLoadingOg = true
            appAdDataOg = null
            KLog.d(Constant.logTagOg, "${getInstanceName()}--广告过期重新加载")
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
        1 to { context, adData ->
            val adType = adData.og_open.getOrNull(adIndexOg)?.og_type
            if (adType == "screen") {
                OgLoadOpenAd.loadStartInsertAdOg(context, adData)
            } else {
                OgLoadOpenAd.loadOpenAdvertisementOg(context, adData)
            }
        },
        2 to { context, adData ->
            OgLoadHomeAd.loadHomeAdvertisementOg(context, adData)
        },
        3 to { context, adData ->
            OgLoadResultAd.loadResultAdvertisementOg(context, adData)
        },
        4 to { context, adData ->
            OgLoadConnectAd.loadConnectAdvertisementOg(context, adData)
        },
        5 to { context, adData ->
            OgLoadBackAd.loadBackAdvertisementOg(context, adData)
        },
        6 to { context, adData ->
            OgLoadListAd.loadListAdvertisementOg(context, adData)
        }
    )

    /**
     * 获取实例名称
     */
    private fun getInstanceName(): String {
        when (id) {
            1 -> {
                return "open"
            }
            2 -> {
                return "home"
            }
            3 -> {
                return "result"
            }
            4 -> {
                return "connect"
            }
            5 -> {
                return "back"
            }
            6 -> {
                return "list"
            }
            else -> {
                return ""
            }
        }
    }
}