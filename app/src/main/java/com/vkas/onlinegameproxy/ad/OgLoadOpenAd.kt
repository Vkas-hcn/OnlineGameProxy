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
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.onlinegameproxy.app.App
import com.vkas.onlinegameproxy.base.AdBase
import com.vkas.onlinegameproxy.bean.OgAdBean
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.key.Constant.logTagOg
import com.vkas.onlinegameproxy.utils.KLog
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.recordNumberOfAdClickOg
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.recordNumberOfAdDisplaysOg
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.takeSortedAdIDOg
import com.xuexiang.xutil.net.JsonUtil
import java.util.*

object OgLoadOpenAd {
    private val adBase = AdBase.getOpenInstance()
    /**
     * 加载启动页广告
     */
    private fun loadStartupPageAdvertisementOg(context: Context, adData: OgAdBean) {
        if (adData.og_open.getOrNull(adBase.adIndexOg)?.og_type == "screen") {
            loadStartInsertAdOg(context, adData)
        } else {
            loadOpenAdvertisementOg(context, adData)
        }
    }

    /**
     * 加载开屏广告
     */
    fun loadOpenAdvertisementOg(context: Context, adData: OgAdBean) {
        KLog.e("loadOpenAdvertisementOg", "adData().og_open=${JsonUtil.toJson(adData.og_open)}")
        KLog.e(
            "loadOpenAdvertisementOg",
            "id=${JsonUtil.toJson(takeSortedAdIDOg(adBase.adIndexOg, adData.og_open))}"
        )

        val id = takeSortedAdIDOg(adBase.adIndexOg, adData.og_open)

        KLog.d(logTagOg, "open--开屏广告id=$id;权重=${adData.og_open.getOrNull(adBase.adIndexOg)?.og_weight}")
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            id,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    adBase.loadTimeOg = Date().time
                    adBase.isLoadingOg = false
                    adBase.appAdDataOg = ad

                    KLog.d(logTagOg, "open--开屏广告加载成功")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    adBase.isLoadingOg = false
                    adBase.appAdDataOg = null
                    if (adBase.adIndexOg < adData.og_open.size - 1) {
                        adBase.adIndexOg++
                        loadStartupPageAdvertisementOg(context, adData)
                    } else {
                        adBase.adIndexOg = 0
                        if(!adBase.isFirstRotation){
                            AdBase.getOpenInstance().advertisementLoadingOg(context)
                            adBase.isFirstRotation =true
                        }
                    }
                    KLog.d(logTagOg, "open--开屏广告加载失败: " + loadAdError.message)
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
                    KLog.d(logTagOg, "open--关闭开屏内容")
                    adBase.whetherToShowOg = false
                    adBase.appAdDataOg = null
                    if (!App.whetherBackgroundOg) {
                        LiveEventBus.get<Boolean>(Constant.OPEN_CLOSE_JUMP)
                            .post(true)
                    }
                }

                //全屏内容无法显示时调用
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    adBase.whetherToShowOg = false
                    adBase.appAdDataOg = null
                    KLog.d(logTagOg, "open--全屏内容无法显示时调用")
                }

                //显示全屏内容时调用
                override fun onAdShowedFullScreenContent() {
                    adBase.appAdDataOg = null
                    adBase.whetherToShowOg = true
                    recordNumberOfAdDisplaysOg()
                    adBase.adIndexOg = 0
                    KLog.d(logTagOg, "open---开屏广告展示")
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    KLog.d(logTagOg, "open---点击open广告")
                    recordNumberOfAdClickOg()
                }
            }
    }

    /**
     * 展示Open广告
     */
    fun displayOpenAdvertisementOg(activity: AppCompatActivity): Boolean {

        if (adBase.appAdDataOg == null) {
            KLog.d(logTagOg, "open---开屏广告加载中。。。")
            return false
        }
        if (adBase.whetherToShowOg || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(logTagOg, "open---前一个开屏广告展示中或者生命周期不对")
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
        val id = takeSortedAdIDOg(adBase.adIndexOg, adData.og_open)
        KLog.d(
            logTagOg,
            "open--插屏广告id=$id;权重=${adData.og_open.getOrNull(adBase.adIndexOg)?.og_weight}"
        )

        InterstitialAd.load(
            context,
            id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { KLog.d(logTagOg, "open---连接插屏加载失败=$it") }
                    adBase.isLoadingOg = false
                    adBase.appAdDataOg = null
                    if (adBase.adIndexOg < adData.og_open.size - 1) {
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
                    KLog.d(logTagOg, "open--启动页插屏加载完成")
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
                    KLog.d(logTagOg, "open--插屏广告点击")
                    recordNumberOfAdClickOg()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLog.d(logTagOg, "open--关闭StartInsert插屏广告${App.isBackDataOg}")
                    if (!App.whetherBackgroundOg) {
                        LiveEventBus.get<Boolean>(Constant.OPEN_CLOSE_JUMP)
                            .post(true)
                    }
                    adBase.appAdDataOg = null
                    adBase.whetherToShowOg = false
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    KLog.d(logTagOg, "Ad failed to show fullscreen content.")
                    adBase.appAdDataOg = null
                    adBase.whetherToShowOg = false
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    KLog.e("TAG", "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    adBase.appAdDataOg = null
                    recordNumberOfAdDisplaysOg()
                    // Called when ad is shown.
                    adBase.whetherToShowOg = true
                    adBase.adIndexOg = 0
                    KLog.d(logTagOg, "open----插屏show")
                }
            }
    }
}