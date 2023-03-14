package com.vkas.onlinegameproxy.ad

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

object OgLoadConnectAd {
    private val adBase = AdBase.getConnectInstance()

    // 广告ID
    var idOg = ""


    /**
     * 加载首页插屏广告
     */
    fun loadConnectAdvertisementOg(context: Context, adData: OgAdBean) {
        val adRequest = AdRequest.Builder().build()
        idOg = takeSortedAdIDOg(adBase.adIndexOg, adData.og_connect)
        KLog.d(
            logTagOg,
            "connect--插屏广告id=$idOg;权重=${adData.og_connect.getOrNull(adBase.adIndexOg)?.og_weight}"
        )

        InterstitialAd.load(
            context,
            idOg,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { KLog.d(logTagOg, "connect---连接插屏加载失败=$it") }
                    adBase.isLoadingOg = false
                    adBase.appAdDataOg = null
                    if (adBase.adIndexOg < adData.og_connect.size - 1) {
                        adBase.adIndexOg++
                        loadConnectAdvertisementOg(context, adData)
                    } else {
                        adBase.adIndexOg = 0
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    adBase.loadTimeOg = Date().time
                    adBase.isLoadingOg = false
                    adBase.appAdDataOg = interstitialAd
                    adBase.adIndexOg = 0
                    KLog.d(logTagOg, "connect---连接插屏加载成功")
                }
            })
    }

    /**
     * connect插屏广告回调
     */
    private fun connectScreenAdCallback() {
        (adBase.appAdDataOg as? InterstitialAd)?.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(logTagOg, "connect插屏广告点击")
                    recordNumberOfAdClickOg()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLog.d(logTagOg, "关闭connect插屏广告=${App.isBackDataOg}")
                    LiveEventBus.get<Boolean>(Constant.PLUG_OG_ADVERTISEMENT_SHOW)
                        .post(App.isBackDataOg)

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
                    KLog.d(logTagOg, "connect----show")
                }
            }
    }

    /**
     * 展示Connect广告
     */
    fun displayConnectAdvertisementOg(activity: AppCompatActivity): Boolean {
        if (adBase.appAdDataOg == null) {
            KLog.d(logTagOg, "connect--插屏广告加载中或超限。。。")
            return false
        }

        if (adBase.whetherToShowOg || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(logTagOg, "connect--前一个插屏广告展示中或者生命周期不对")
            return false
        }
        connectScreenAdCallback()
        activity.lifecycleScope.launch(Dispatchers.Main) {
            if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                (adBase.appAdDataOg as InterstitialAd).show(activity)
            }
        }
        return true
    }
}