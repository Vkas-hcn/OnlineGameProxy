package op.asd.ad

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
import com.lsxiao.apollo.core.Apollo
import b.C
import op.asd.base.AdBase
import op.asd.bean.OgAdBean
import op.asd.bean.OgDetailBean
import op.asd.key.Constant
import op.asd.key.Constant.logTagOg
import op.asd.utils.KLogUtils
import op.asd.utils.OnlineGameUtils
import op.asd.utils.OnlineGameUtils.recordNumberOfAdClickOg
import op.asd.utils.OnlineGameUtils.recordNumberOfAdDisplaysOg
import op.asd.utils.OnlineGameUtils.takeSortedAdIDOg
import op.asd.utils.OnlineGameUtils.whetherItIsABlacklist
import op.asd.utils.OnlineOkHttpUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

object OgLoadBackAd {
    private val adBase = AdBase.getBackInstance()
    // 广告ID
    var idOg = ""
    var ogDetailBean: OgDetailBean? = null


    /**
     * 加载首页插屏广告
     */
    fun loadBackAdvertisementOg(context: Context, adData: OgAdBean) {
        val adRequest = AdRequest.Builder().build()
        ogDetailBean = OnlineGameUtils.beforeLoadLinkSettingsOg(adData.ongpro_i_2H.getOrNull(adBase.adIndexOg))

        idOg = takeSortedAdIDOg(adBase.adIndexOg, adData.ongpro_i_2H)
        KLogUtils.d( "back--插屏广告id=$idOg;权重=${adData.ongpro_i_2H.getOrNull(adBase.adIndexOg)?.ongpro_y}")

        InterstitialAd.load(
            context,
            idOg,
            adRequest,
            interstitialAdLoadCallback(context,adData)
        )
    }

    private fun interstitialAdLoadCallback(context: Context,adData: OgAdBean): InterstitialAdLoadCallback {
        return object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                adError.toString().let {
                    KLogUtils.d( "back---连接插屏加载失败=$it")
                }
                adBase.isLoadingOg = false
                adBase.appAdDataOg = null
                if (adBase.adIndexOg < adData.ongpro_i_2H.size - 1) {
                    adBase.adIndexOg++
                    loadBackAdvertisementOg(context, adData)
                } else {
                    adBase.adIndexOg = 0
                }
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                adBase.loadTimeOg = Date().time
                adBase.isLoadingOg = false
                adBase.appAdDataOg = interstitialAd
                adBase.adIndexOg = 0
                KLogUtils.d("back---返回插屏加载成功")
                interstitialAd.setOnPaidEventListener { adValue ->
                    OnlineOkHttpUtils.postAdEvent(
                        adValue,
                        interstitialAd.responseInfo, ogDetailBean, "interstitial", "ongpro_i_2H"
                    )
                }
                backScreenAdCallback()
            }
        }
    }

    /**
     * back插屏广告回调
     */
    private fun backScreenAdCallback() {
        (adBase.appAdDataOg  as? InterstitialAd)?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                KLogUtils.d("back插屏广告点击")
                recordNumberOfAdClickOg()
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                KLogUtils.d("关闭back插屏广告${C.isBackDataOg}")
                Apollo.emit(Constant.PLUG_OG_BACK_AD_SHOW, C.isBackDataOg, true)

                adBase.appAdDataOg = null
                adBase.whetherToShowOg = false
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                // Called when ad fails to show.
                KLogUtils.d("Ad failed to show fullscreen content.")
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
                KLogUtils.d("back----show")
                ogDetailBean = OnlineGameUtils.afterLoadLinkSettingsOg(ogDetailBean)
            }
        }
    }


    /**
     * 展示Connect广告
     */
    fun displayBackAdvertisementOg(activity: AppCompatActivity): Int {
        val localVpnBootData = OnlineGameUtils.getLocalVpnBootData()
        val blacklistUser = C.mmkvOg.decodeString(Constant.BLACKLIST_USER_OG, "")
        val blacklistUserBool = whetherItIsABlacklist(blacklistUser?:"")
        KLogUtils.d("bubble_cloak---${localVpnBootData.online_cloak}。。。")
        KLogUtils.d("blacklist_user---${blacklistUserBool}。。。")

        if (blacklistUserBool && localVpnBootData.online_cloak == "1") {
            KLogUtils.d("根据黑名单屏蔽插屏广告。。。")
            return 0
        }
        if(!OnlineGameUtils.whetherToBlockScreenAds(localVpnBootData.online_ref)){
            KLogUtils.d("根据买量屏蔽插屏广告。。。")
            return 0
        }
        if (adBase.appAdDataOg == null) {
            KLogUtils.d("back--插屏广告加载中。。。")
            return 1
        }
        if (adBase.whetherToShowOg || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLogUtils.d("back--前一个插屏广告展示中或者生命周期不对")
            return 1
        }
        backScreenAdCallback()
        activity.lifecycleScope.launch(Dispatchers.Main) {
            (adBase.appAdDataOg as InterstitialAd).show(activity)
        }
        return 2
    }
}