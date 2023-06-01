package com.vkas.onlinegameproxy.ad

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.lsxiao.apollo.core.Apollo
import com.vkas.onlinegameproxy.app.App
import com.vkas.onlinegameproxy.base.AdBase
import com.vkas.onlinegameproxy.bean.OgAdBean
import com.vkas.onlinegameproxy.bean.OgDetailBean
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.key.Constant.logTagOg
import com.vkas.onlinegameproxy.utils.KLogUtils
import com.vkas.onlinegameproxy.utils.OnlineGameUtils
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.recordNumberOfAdClickOg
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.recordNumberOfAdDisplaysOg
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.takeSortedAdIDOg
import com.vkas.onlinegameproxy.utils.OnlineOkHttpUtils
import com.xuexiang.xutil.net.JsonUtil
import java.util.*

object OgLoadOpenAd {
    private val adBase = AdBase.getOpenInstance()
    // 广告ID
    var idOg = ""
    var ogDetailBean: OgDetailBean? = null
    /**
     * 加载启动页广告
     */
    private fun loadStartupPageAdvertisementOg(context: Context, adData: OgAdBean) {
        if (adData.ongpro_o_open.getOrNull(adBase.adIndexOg)?.ongpro_type == "screen") {
            loadStartInsertAdOg(context, adData)
        } else {
            loadOpenAdvertisementOg(context, adData)
        }
    }

    /**
     * 加载开屏广告
     */
    fun loadOpenAdvertisementOg(context: Context, adData: OgAdBean) {
        KLogUtils.e( "adData().ongpro_o_open=${JsonUtil.toJson(adData.ongpro_o_open)}")
        KLogUtils.e(
            "id=${JsonUtil.toJson(takeSortedAdIDOg(adBase.adIndexOg, adData.ongpro_o_open))}"
        )
        ogDetailBean = OnlineGameUtils.beforeLoadLinkSettingsOg(
            adData.ongpro_o_open.getOrNull(
                adBase.adIndexOg
            )
        )
        idOg = takeSortedAdIDOg(adBase.adIndexOg, adData.ongpro_o_open)

        KLogUtils.d( "open--开屏广告id=$idOg;权重=${adData.ongpro_o_open.getOrNull(adBase.adIndexOg)?.ongpro_y}")
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            idOg,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    ad.setOnPaidEventListener { adValue ->
                        KLogUtils.e( "开屏页--开屏广告开始上报")
                        OnlineOkHttpUtils.postAdEvent(
                            adValue,
                            ad.responseInfo, ogDetailBean, "open", "ongpro_o_open"
                        )
                    }
                    adBase.loadTimeOg = Date().time
                    adBase.isLoadingOg = false
                    adBase.appAdDataOg = ad
                    KLogUtils.d( "open--开屏广告加载成功")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    adBase.isLoadingOg = false
                    adBase.appAdDataOg = null
                    if (adBase.adIndexOg < adData.ongpro_o_open.size - 1) {
                        adBase.adIndexOg++
                        loadStartupPageAdvertisementOg(context, adData)
                    } else {
                        adBase.adIndexOg = 0
                        if(!adBase.isFirstRotation){
                            AdBase.getOpenInstance().advertisementLoadingOg(context)
                            adBase.isFirstRotation =true
                        }
                    }
                    KLogUtils.d( "open--开屏广告加载失败: " + loadAdError.message)
                }
            }
        )
    }


    /**
     * 开屏广告回调
     */
    private fun advertisingOpenCallbackOg() {
        if (adBase.appAdDataOg !is AppOpenAd) {
            return
        }
        (adBase.appAdDataOg as AppOpenAd).fullScreenContentCallback =
            object : FullScreenContentCallback() {
                //取消全屏内容
                override fun onAdDismissedFullScreenContent() {
                    KLogUtils.d( "open--关闭开屏内容")
                    adBase.whetherToShowOg = false
                    adBase.appAdDataOg = null
                    if (!App.whetherBackgroundOg) {
                        Apollo.emit(Constant.OPEN_CLOSE_JUMP, actual = true, sticky = true)
                    }
                }

                //全屏内容无法显示时调用
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    adBase.whetherToShowOg = false
                    adBase.appAdDataOg = null
                    KLogUtils.d( "open--全屏内容无法显示时调用")
                }

                //显示全屏内容时调用
                override fun onAdShowedFullScreenContent() {
                    adBase.appAdDataOg = null
                    adBase.whetherToShowOg = true
                    recordNumberOfAdDisplaysOg()
                    adBase.adIndexOg = 0
                    KLogUtils.d( "open---开屏广告展示")
                    ogDetailBean = OnlineGameUtils.afterLoadLinkSettingsOg(ogDetailBean)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    KLogUtils.d( "open---点击open广告")
                    recordNumberOfAdClickOg()
                }
            }
    }

    /**
     * 展示Open广告
     */
    fun displayOpenAdvertisementOg(activity: AppCompatActivity): Boolean {

        if (adBase.appAdDataOg == null) {
            KLogUtils.d( "open---开屏广告加载中。。。")
            return false
        }
        if (adBase.whetherToShowOg || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLogUtils.d( "open---前一个开屏广告展示中或者生命周期不对")
            return false
        }
        if (adBase.appAdDataOg is AppOpenAd) {
            advertisingOpenCallbackOg()
            (adBase.appAdDataOg as AppOpenAd).show(activity)
        } else {
            startInsertScreenAdCallbackOg()
            (adBase.appAdDataOg as InterstitialAd).show(activity)
        }
        return true
    }

    /**
     * 加载启动页插屏广告
     */
    fun loadStartInsertAdOg(context: Context, adData: OgAdBean) {
        val adRequest = AdRequest.Builder().build()
        val id = takeSortedAdIDOg(adBase.adIndexOg, adData.ongpro_o_open)
        KLogUtils.d(

            "open--插屏广告id=$id;权重=${adData.ongpro_o_open.getOrNull(adBase.adIndexOg)?.ongpro_y}"
        )

        InterstitialAd.load(
            context,
            id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { KLogUtils.d( "open---连接插屏加载失败=$it") }
                    adBase.isLoadingOg = false
                    adBase.appAdDataOg = null
                    if (adBase.adIndexOg < adData.ongpro_o_open.size - 1) {
                        adBase.adIndexOg++
                        loadStartupPageAdvertisementOg(context, adData)
                    } else {
                        adBase.adIndexOg = 0
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    adBase.loadTimeOg = Date().time
                    adBase.isLoadingOg = false
                    adBase.appAdDataOg = interstitialAd
                    KLogUtils.d( "open--启动页插屏加载完成")
                }
            })
    }

    /**
     * StartInsert插屏广告回调
     */
    private fun startInsertScreenAdCallbackOg() {
        if (adBase.appAdDataOg !is InterstitialAd) {
            return
        }
        (adBase.appAdDataOg as InterstitialAd).fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLogUtils.d( "open--插屏广告点击")
                    recordNumberOfAdClickOg()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLogUtils.d( "open--关闭StartInsert插屏广告${App.isBackDataOg}")
                    if (!App.whetherBackgroundOg) {
                        Apollo.emit(Constant.OPEN_CLOSE_JUMP, actual = true, sticky = true)

                    }
                    adBase.appAdDataOg = null
                    adBase.whetherToShowOg = false
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    KLogUtils.d( "Ad failed to show fullscreen content.")
                    adBase.appAdDataOg = null
                    adBase.whetherToShowOg = false
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                }

                override fun onAdShowedFullScreenContent() {
                    adBase.appAdDataOg = null
                    recordNumberOfAdDisplaysOg()
                    // Called when ad is shown.
                    adBase.whetherToShowOg = true
                    adBase.adIndexOg = 0
                    KLogUtils.d( "open----插屏show")
                }
            }
    }
}