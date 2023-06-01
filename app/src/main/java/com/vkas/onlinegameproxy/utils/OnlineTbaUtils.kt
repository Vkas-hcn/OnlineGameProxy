package com.vkas.onlinegameproxy.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import android.webkit.WebSettings
import com.android.installreferrer.api.ReferrerDetails
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.RomUtils
import com.blankj.utilcode.util.ThreadUtils.runOnUiThread
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.gson.reflect.TypeToken
import com.vkas.onlinegameproxy.app.App.Companion.mmkvOg
import com.vkas.onlinegameproxy.bean.OgDetailBean
import com.vkas.onlinegameproxy.key.Constant
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.system.DeviceUtils
import org.json.JSONObject
import com.xuexiang.xutil.app.AppUtils
import com.xuexiang.xutil.data.DateUtils
import com.xuexiang.xutil.display.ScreenUtils
import com.xuexiang.xutil.net.NetworkUtils
import java.util.*

object OnlineTbaUtils {
    /**
     * 顶层json
     */
    fun getTopLevelJsonData(
        isAd: Boolean = false,
        ogDetailBean: OgDetailBean? = null
    ): JSONObject {
        return JSONObject().apply {
            if (isAd) {
                val bubbleLoadCity = ogDetailBean?.ongpro_load_city ?: "null"
                val bubbleShowCity = ogDetailBean?.ongpro_show_city ?: "null"
                put("when&r_city", bubbleLoadCity)
                put("when&s_city", bubbleShowCity)
            }
            put("twill", JSONObject().apply {
                //gaid
                put(
                    "elapse",
                    mmkvOg.decodeString(Constant.GOOGLE_ADVERTISING_ID_OG, null)
                )//原值，google广告id
                //manufacturer
                put("taxi", DeviceUtils.getManufacturer())//手机厂商，huawei、oppo
                //ip
                put("pier", mmkvOg.decodeString(Constant.CURRENT_IP_OG, null))
                //distinct_id
                put("garry", mmkvOg.decodeString(Constant.UUID_VALUE_OG, null))//用户排重字段
                //network_type
                put(
                    "whoop",
                    NetworkUtils.getNetStateType().toString().replaceFirst("NET_", "")
                        .lowercase(Locale.getDefault())
                )//网络类型：wifi，3g等，非必须，和产品确认是否需要分析网络类型相关的信息，此参数可能需要系统权限
                //log_id
                put("dovetail", UUID.randomUUID().toString())
                //os_version
                put("fake", (RomUtils.getRomInfo().version) ?: "")//操作系统版本号
                //client_ts
                put("salivate", DateUtils.getNowMills())//日志发生的客户端时间，毫秒数
                //brand
                put("ohare", "")//品牌
                //device_model
                put("artisan", DeviceUtils.getDeviceModel())//手机型号
                //app_version
                put("stingray", AppUtils.getAppVersionName())//应用的版本
                //screen_dpi
                put("uganda", ScreenUtils.getScreenDensity().toString())//屏幕像素密度
                //os
                put(
                    "flinty",
                    "executor"
                )//操作系统：{“executor”: “android”, “pinwheel”: “ios”, “quality”: “web”}
                //sdk_ver
                put("atalanta", DeviceUtils.getSDKVersionName())//安卓sdk版本号，数字
                //os_country
                put("knives", Locale.getDefault().country)//操作系统中的国家简写，例如 CN，US等
            })
            put("angela", JSONObject().apply {
                //zone_offset
                put(
                    "laud",
                    TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000
                )//客户端时区
                //system_language
                put(
                    "millie",
                    "${Locale.getDefault().language}_${Locale.getDefault().country}"
                )// String locale = Locale.getDefault(); 拼接为：zh_CN的形式，下杠
                //operator
                put(
                    "quotient",
                    com.blankj.utilcode.util.NetworkUtils.getNetworkOperatorName()
                )//网络供应商名称
                //android_id
                put("cyril", DeviceUtils.getAndroidID())
                //battery_left
                put("slog", getBatteryLevel(XUtil.getContext()))
                //bundle_id
                put("cockcrow", AppUtils.getAppPackageName())//当前的包名称，a.b.c
            })
        }
    }

    fun getSessionJson(): String {
        return getTopLevelJsonData().apply {
            put("flounce", JSONObject().apply {})
        }.toString()
    }

    fun getAdJson(
        adValue: AdValue,
        responseInfo: ResponseInfo,
        ogDetailBean: OgDetailBean?,
        adType: String,
        adKey: String
    ): String {

        return getTopLevelJsonData(true, ogDetailBean).apply {
            put("pitfall", JSONObject().apply {
                //ad_pre_ecpm
                put("quanta", adValue.valueMicros)//价格
                //currency
                put("kline", adValue.currencyCode)//预估收益的货币单位
                //ad_network
                put(
                    "phosphor",
                    responseInfo.mediationAdapterClassName
                )//广告网络，广告真实的填充平台，例如admob的bidding，填充了Facebook的广告，此值为Facebook
                //ad_source
                put("tower", "admob")
                //ad_code_id
                put("useful", ogDetailBean?.ongpro_id)
                //ad_pos_id
                put("cage", adKey)
                //ad_rit_id
                put("basalt", null)
                //ad_sense
                put("me", null)
                //ad_format
                put("radices", adType)
                //precision_type
                put("strong", getPrecisionType(adValue.precisionType))
                //ad_load_ip
                put("ethyl", ogDetailBean?.ongpro_load_ip ?: "")
                //ad_impression_ip
                put("precious", ogDetailBean?.ongpro_show_ip ?: "")
                //ad_sdk_ver
                put("flaxen", responseInfo.responseId)
            })
        }.toString()
    }

    fun install(context: Context, referrerDetails: ReferrerDetails): String {
        return getTopLevelJsonData().apply {
            put("dad", JSONObject().apply {
                put("quota", "ebony")
                //build
                put("abide", "build/${Build.ID}")

                //referrer_url
                put("shiplap", referrerDetails.installReferrer)

                //install_version
                put("thinnish", referrerDetails.installVersion)

                //user_agent
                put("brevity", getMyDefaultUserAgent(context))

                //lat
                put("godwit", getLimitTracking(context))

                //referrer_click_timestamp_seconds
                put("duress", referrerDetails.referrerClickTimestampSeconds)

                //install_begin_timestamp_seconds
                put("pullman", referrerDetails.installBeginTimestampSeconds)

                //referrer_click_timestamp_server_seconds
                put("titus", referrerDetails.referrerClickTimestampServerSeconds)

                //install_begin_timestamp_server_seconds
                put("gangway", referrerDetails.installBeginTimestampServerSeconds)

                //install_first_seconds
                put("montreal", timeTheAppWasFirstInstalled(context))

                //last_update_seconds
                put("ecliptic", timeLastUpdateWasApplied(context))

                //google_play_instant
                put("lifo", referrerDetails.googlePlayInstantParam)
            })

        }.toString()
    }

    /**
     * cloak
     */
    fun cloakJson(): Map<String, Any> {
        return mapOf<String, Any>(
            //distinct_id
            "garry" to (mmkvOg.decodeString(Constant.UUID_VALUE_OG, null) ?: ""),
            //client_ts
            "salivate" to (DateUtils.getNowMills()),//日志发生的客户端时间，毫秒数
            //device_model
            "artisan" to DeviceUtils.getDeviceModel(),
            //bundle_id
            "cockcrow" to (AppUtils.getAppPackageName()),//当前的包名称，a.b.c
            //os_version
            "fake" to RomUtils.getRomInfo().version,
            //gaid
            "elapse" to (mmkvOg.decodeString(Constant.GOOGLE_ADVERTISING_ID_OG, null)?:""),
            //android_id
            "cyril" to DeviceUtils.getAndroidID(),
            //os
            "flinty" to "executor",
            //app_version
            "stingray" to AppUtils.getAppVersionName(),//应用的版本
            //brand
            "ohare" to ""//品牌
        )
    }


    /**
     * 获取IP地址（https://ifconfig.me/ip）
     */
    fun obtainIpAddress() {
        OnlineOkHttpUtils.getCurrentIp()
    }

    /**
     * 获取Google广告ID
     */
    fun obtainGoogleAdvertisingId(activity: Activity) {
        runCatching {
            val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(activity)
            MmkvUtils.set(Constant.GOOGLE_ADVERTISING_ID_OG, adInfo.id)
            KLogUtils.e( "googleAdId---->${adInfo.id}")
        }.getOrNull()
    }

    fun getNetworkOperator(context: Context): String {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return telephonyManager.networkOperatorName
    }

    /**
     * 获取用户是否启用了限制跟踪(IO使用)
     */
    private fun getLimitTracking(context: Context): String {
        return try {
            if (AdvertisingIdClient.getAdvertisingIdInfo(context).isLimitAdTrackingEnabled) {
                "offset"
            } else {
                "sob"
            }
        } catch (e: Exception) {
            "sob"
        }
    }

    /**
     * 应用首次安装的时间
     */
    private fun timeTheAppWasFirstInstalled(context: Context): Long {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return (packageInfo.firstInstallTime / 1000L)
    }

    /**
     * 应用最后一次更新的时间
     */
    private fun timeLastUpdateWasApplied(context: Context): Long {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return (packageInfo.lastUpdateTime / 100L)
    }

    /**
     * precisionType索引
     */
    private fun getPrecisionType(precisionType: Int): String {
        return when (precisionType) {
            0 -> {
                "UNKNOWN"
            }
            1 -> {
                "ESTIMATED"
            }
            2 -> {
                "PUBLISHER_PROVIDED"
            }
            3 -> {
                "PRECISE"
            }
            else -> {
                "UNKNOWN"
            }
        }
    }

    /**
     * 获取getDefaultUserAgent值
     */
    private fun getMyDefaultUserAgent(context: Context): String {
        return try {
            WebSettings.getDefaultUserAgent(context)
        } catch (e: Exception) {
            ""
        }
    }

    private fun getBatteryLevel(context: Context): Int {
        val batteryLevel: Int
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            ifilter.addAction(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(null, ifilter)
        }
        batteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        return batteryLevel
    }
}