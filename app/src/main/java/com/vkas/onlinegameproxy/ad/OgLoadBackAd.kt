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

object OgLoadBackAd {
    private val adBase = AdBase.getBackInstance()

    /**
     * 加载首页插屏广告
     */
    fun loadBackAdvertisementOg(context: Context, adData: OgAdBean) {
        val adRequest = AdRequest.Builder().build()
        val id = takeSortedAdIDOg(adBase.adIndexOg, adData.og_back)
        KLog.d(logTagOg, "back--插屏广告id=$id;权重=${adData.og_back.getOrNull(adBase.adIndexOg)?.og_weight}")

        InterstitialAd.load(
            context,
            id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let {
                        KLog.d(logTagOg, "back---连接插屏加载失败=$it") }
                    adBase.isLoadingOg = false
                    adBase.appAdDataOg = null
                    if (adBase.adIndexOg < adData.og_back.size - 1) {
                        adBase.adIndexOg++
                        loadBackAdvertisementOg(context,adData)
                    }else{
                        adBase.adIndexOg = 0
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    adBase.loadTimeOg = Date().time
                    adBase.isLoadingOg = false
                    adBase.appAdDataOg = interstitialAd
                    adBase.adIndexOg = 0
                    KLog.d(logTagOg, "back---返回插屏加载成功")
                }
            })
    }

    /**
     * back插屏广告回调
     */
    private fun backScreenAdCallback() {
        (adBase.appAdDataOg  as? InterstitialAd)?.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(logTagOg, "back插屏广告点击")
                    recordNumberOfAdClickOg()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLog.d(logTagOg, "关闭back插屏广告${App.isBackDataOg}")
                    LiveEventBus.get<Boolean>(Constant.PLUG_OG_BACK_AD_SHOW)
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
                    KLog.d(logTagOg, "back----show")
                }
            }
    }

    /**
     * 展示Connect广告
     */
    fun displayBackAdvertisementOg(activity: AppCompatActivity): Boolean {
        if (adBase.appAdDataOg == null) {
            KLog.d(logTagOg, "back--插屏广告加载中。。。")
            return false
        }
        if (adBase.whetherToShowOg || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(logTagOg, "back--前一个插屏广告展示中或者生命周期不对")
            return false
        }
        backScreenAdCallback()
        activity.lifecycleScope.launch(Dispatchers.Main) {
            (adBase.appAdDataOg as InterstitialAd).show(activity)
        }
        return true
    }
}