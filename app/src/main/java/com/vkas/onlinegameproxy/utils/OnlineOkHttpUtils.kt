package com.vkas.onlinegameproxy.utils
import android.content.Context
import com.android.installreferrer.api.ReferrerDetails
import com.blankj.utilcode.util.LogUtils
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.ResponseInfo
import com.vkas.onlinegameproxy.BuildConfig
import com.vkas.onlinegameproxy.bean.OgDetailBean
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.net.HttpApi
import com.vkas.onlinegameproxy.net.IHttpCallback
import com.vkas.onlinegameproxy.net.OkHttpApi

import com.xuexiang.xutil.tip.ToastUtils
import com.xuexiang.xutil.app.AppUtils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.system.DeviceUtils
object OnlineOkHttpUtils {
    private val httpApi: HttpApi = OkHttpApi()
    private var urlService = if (BuildConfig.DEBUG) {
        Constant.SERVER_DISTRIBUTION_ADDRESS_TEST_OG
    } else {
        Constant.SERVER_DISTRIBUTION_ADDRESS_OG
    }
    var urlTba = if (BuildConfig.DEBUG) {
        Constant.TBA_ADDRESS_TEST_OG
    } else {
        Constant.TBA_ADDRESS_OG
    }

    fun getCurrentIp() {
        httpApi.get(
            emptyMap(),
            "https://ifconfig.me/ip",
            object : IHttpCallback {
                override fun onSuccess(data: Any?) {
                    LogUtils.d("success result : ${data.toString()}")
                    KLog.e("TAG", "IP----->${data}")
                    MmkvUtils.set(Constant.CURRENT_IP_OG, data.toString())
                }

                override fun onFailed(error: Any?) {
                    KLog.e("TAG", "IP--code Exception")
                    MmkvUtils.set(Constant.CURRENT_IP_OG, "")
                }
            })
    }

    /**
     * session事件上报
     */
    fun postSessionEvent() {
        val json = OnlineTbaUtils.getSessionJson()
        KLog.e("TBA", "json--session-->${json}")
        httpApi.post(
            json,
            urlTba,
            object : IHttpCallback {
                override fun onSuccess(data: Any?) {
                    KLog.e("TBA", "session事件上报-成功->")
                }

                override fun onFailed(error: Any?) {
                    MmkvUtils.set(Constant.SESSION_JSON_OG, json)
                    KLog.e("TBA", "session事件上报-失败-->${error}")
                }
            })
    }


    /**
     * install事件上报
     */
    fun postInstallEvent(context: Context, referrerDetails: ReferrerDetails) {
        val json = OnlineTbaUtils.install(context, referrerDetails)
        KLog.e("TBA", "json-install--->${json}")
        httpApi.post(
            json,
            urlTba,
            object : IHttpCallback {
                override fun onSuccess(data: Any?) {
                    KLog.e("TBA", "install事件上报-成功->")
                    MmkvUtils.set(Constant.INSTALL_TYPE_OG, true)

                }

                override fun onFailed(error: Any?) {
                    MmkvUtils.set(Constant.INSTALL_TYPE_OG, false)
                    KLog.e("TBA", "install事件上报-失败-->${error}")
                }
            })
    }

    /**
     * 广告事件上报
     */
    fun postAdEvent(
        adValue: AdValue,
        responseInfo: ResponseInfo,
        ogDetailBean: OgDetailBean?,
        adType: String,
        adKey: String,
    ) {
        val json = OnlineTbaUtils.getAdJson(adValue, responseInfo, ogDetailBean, adType, adKey)
        KLog.e("TBA", "json-Ad---$adKey---->${json}")

        httpApi.post(
            json,
            urlTba,
            object : IHttpCallback {
                override fun onSuccess(data: Any?) {
                    KLog.e("TBA", "${adKey}广告事件上报-成功->")
                }

                override fun onFailed(error: Any?) {
                    KLog.e("TBA", "${adKey}广告事件上报-失败-->${error}")
                }
            })
    }

    /**
     * Cloak接入，获取黑名单
     */
    fun getBlacklistData() {
//        if (BuildConfig.DEBUG) {
//            return
//        }
        val params = OnlineTbaUtils.cloakJson()
        KLog.e("TBA","json--黑名单-->${JsonUtil.toJson(params)}")
        httpApi.get(
            params,
            Constant.cloak_url_OG,
            object : IHttpCallback {
                override fun onSuccess(data: Any?) {
                    KLog.e("TBA", "Cloak接入--成功--->${data}")
                    if (data == "flare") {
                        MmkvUtils.set(Constant.BLACKLIST_USER_OG, true)
                    } else {
                        MmkvUtils.set(Constant.BLACKLIST_USER_OG, false)
                    }
                }

                override fun onFailed(error: Any?) {
                    KLog.e("TBA", "Cloak接入--失败-- $error")
                    MmkvUtils.set(Constant.BLACKLIST_USER_OG, true)
                }
            }, true
        )
    }

    /**
     * 获取下发数据
     */
    fun getDeliverData() {
        httpApi.get(
            mapOf(),
            urlService,
            object : IHttpCallback {
                override fun onSuccess(data: Any?) {
                    val fastData = OnlineGameUtils.sendResultDecoding(data as String)
                    MmkvUtils.set(Constant.SEND_SERVER_DATA, fastData)
                    KLog.e("TBA", "获取下发服务器数据-成功->${fastData}")
                    val data = OnlineGameUtils.getDataFromTheServer()
                    val json = JsonUtil.toJson(data)
                    KLog.e("TBA","下发服务器数据-json=$json")
                }

                override fun onFailed(error: Any?) {
                    MmkvUtils.set(Constant.SEND_SERVER_DATA, "")
                    KLog.e("TBA", "获取下发服务器数据-失败->${error}")
                }
            })
    }

    /**
     * 心跳上报
     */
    fun getHeartbeatReporting(lieutenants: String, ss_ip: String) {
        //包名
        val lot = AppUtils.getAppPackageName()
        // 版本号
        val island = AppUtils.getAppVersionName()
        //设备ID
        val rifle = DeviceUtils.getAndroidID()
        //协议名称
        val ship = "SS"
        val urlParams =
            "https://${ss_ip}/sdft/hio/?bandages=${lot}&island=${island}&rifle=$rifle&lieutenants=$lieutenants&ship=$ship"
        httpApi.get(
            mapOf(),
            urlParams,
            object : IHttpCallback {
                override fun onSuccess(data: Any?) {
                    KLog.e("TBA", "心跳上报-成功->${data}")
                }

                override fun onFailed(error: Any?) {
                    KLog.e("TBA", "心跳上报-失败->${error}")
                }
            })
    }
}