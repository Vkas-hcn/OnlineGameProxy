package op.asd.utils

import android.content.Context
import com.android.installreferrer.api.ReferrerDetails
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.ResponseInfo
import op.asd.BuildConfig
import b.C.Companion.mmkvOg
import op.asd.bean.OgDetailBean
import op.asd.key.Constant
import com.xuexiang.xutil.app.AppUtils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.system.DeviceUtils
import op.asd.key.Constant.cloak_url_OG
import op.asd.net.*


object OnlineOkHttpUtils {
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
    val client = OkHttpClientWrapper()

    fun getCurrentIp() {
        try {
            val url = "https://ifconfig.me/ip"
            client.get(url, object : OkHttpClientWrapper.Callback {
                override fun onSuccess(response: String) {
                    KLogUtils.e( "IP----->${response}")
                    MmkvUtils.set(Constant.CURRENT_IP_OG, response)
                }

                override fun onFailure(error: String) {
                    KLogUtils.e( "IP--code Exception")
                    MmkvUtils.set(Constant.CURRENT_IP_OG, "")
                }
            })
        } catch (e: Exception) {

        }

    }

    /**
     * session事件上报
     */
    fun postSessionEvent() {
        val json = OnlineTbaUtils.getSessionJson()
        KLogUtils.e( "json--session-->${json}")
        try {
            client.post(urlTba, json, object : OkHttpClientWrapper.Callback {
                override fun onSuccess(response: String) {
                    KLogUtils.e( "session事件上报-成功->")

                }

                override fun onFailure(error: String) {
                    MmkvUtils.set(Constant.SESSION_JSON_OG, json)
                    KLogUtils.e( "session事件上报-失败-->${error}")
                }
            })
        } catch (e: Exception) {

        }

    }


    /**
     * install事件上报
     */
    fun postInstallEvent(context: Context, referrerDetails: ReferrerDetails) {
        val json = OnlineTbaUtils.install(context, referrerDetails)
        KLogUtils.e( "json-install--->${json}")
        try {
            client.post(urlTba, json, object : OkHttpClientWrapper.Callback {
                override fun onSuccess(response: String) {
                    KLogUtils.e("install事件上报-成功->")
                    MmkvUtils.set(Constant.INSTALL_TYPE_OG, true)
                }

                override fun onFailure(error: String) {
                    MmkvUtils.set(Constant.INSTALL_TYPE_OG, false)
                    KLogUtils.e("install事件上报-失败-->${error}")
                }
            })
        } catch (e: Exception) {

        }

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
        KLogUtils.e( "json-Ad---$adKey---->${json}")
        try {
            client.post(urlTba, json, object : OkHttpClientWrapper.Callback {
                override fun onSuccess(response: String) {
                    KLogUtils.e("${adKey}广告事件上报-成功->")
                }

                override fun onFailure(error: String) {
                    KLogUtils.e( "${adKey}广告事件上报-失败-->${error}")
                }
            })
        } catch (e: Exception) {

        }

    }

    /**
     * Cloak接入，获取黑名单
     */
    fun getBlacklistData() {
//        if (BuildConfig.DEBUG) {
//            return
//        }
        val data = mmkvOg.decodeString(Constant.BLACKLIST_USER_OG, "")
        if (!data.isNullOrBlank()) {
            return
        }
        val params = OnlineTbaUtils.cloakJson()
        KLogUtils.e("json--黑名单-->${JsonUtil.toJson(params)}")

        try {
            client.getMap(cloak_url_OG, params, object : OkHttpClientWrapper.Callback {
                override fun onSuccess(response: String) {
                    KLogUtils.e( "Cloak接入--成功--->${response}")
                    MmkvUtils.set(Constant.BLACKLIST_USER_OG, response)
                }

                override fun onFailure(error: String) {
                    KLogUtils.e("Cloak接入--失败-- $error")
                    MmkvUtils.set(Constant.BLACKLIST_USER_OG, "")
                }
            })
        } catch (e: Exception) {

        }

    }

    /**
     * 获取下发数据
     */
    fun getDeliverData() {
        try {
            client.get(urlService, object : OkHttpClientWrapper.Callback {
                override fun onSuccess(response: String) {
                    val fastData = OnlineGameUtils.sendResultDecoding(response)
                    MmkvUtils.set(Constant.SEND_SERVER_DATA, fastData)
                    KLogUtils.e( "获取下发服务器数据-成功->${fastData}")
                    val data = OnlineGameUtils.getDataFromTheServer()
                    val json = JsonUtil.toJson(data)
                    KLogUtils.e( "下发服务器数据-json=$json")
                }

                override fun onFailure(error: String) {
                    MmkvUtils.set(Constant.SEND_SERVER_DATA, "")
                    KLogUtils.e("获取下发服务器数据-失败->${error}")
                }
            })
        } catch (e: Exception) {

        }

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

        try {
            client.get(urlParams, object : OkHttpClientWrapper.Callback {
                override fun onSuccess(response: String) {
                    KLogUtils.e( "心跳上报-成功->${response}")
                }

                override fun onFailure(error: String) {
                    KLogUtils.e( "心跳上报-失败->${error}")
                }
            })
        } catch (e: Exception) {

        }

    }
}