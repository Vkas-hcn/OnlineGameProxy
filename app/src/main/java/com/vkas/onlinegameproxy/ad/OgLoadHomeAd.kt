package com.vkas.onlinegameproxy.ad

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.vkas.onlinegameproxy.base.AdBase
import com.vkas.onlinegameproxy.bean.OgAdBean
import com.vkas.onlinegameproxy.databinding.ActivityMainBinding
import com.vkas.onlinegameproxy.key.Constant.logTagOg
import com.vkas.onlinegameproxy.utils.KLog
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.recordNumberOfAdClickOg
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.takeSortedAdIDOg
import java.util.*
import com.vkas.onlinegameproxy.R
import com.vkas.onlinegameproxy.app.App
import com.vkas.onlinegameproxy.bean.OgDetailBean
import com.vkas.onlinegameproxy.utils.OnlineGameUtils
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.recordNumberOfAdDisplaysOg
import com.vkas.onlinegameproxy.utils.OnlineOkHttpUtils
import com.vkas.onlinegameproxy.utils.RoundCornerOutlineProvider

object OgLoadHomeAd {
    private val adBase = AdBase.getHomeInstance()
    // 广告ID
    var idOg = ""
    var ogDetailBean: OgDetailBean? = null
    /**
     * 加载vpn原生广告
     */
    fun loadHomeAdvertisementOg(context: Context, adData: OgAdBean) {
        ogDetailBean = OnlineGameUtils.beforeLoadLinkSettingsOg(adData.ongpro_n_home.getOrNull(adBase.adIndexOg))
        idOg = takeSortedAdIDOg(adBase.adIndexOg, adData.ongpro_n_home)
        KLog.d(logTagOg, "home---原生广告id=$idOg;权重=${adData.ongpro_n_home.getOrNull(adBase.adIndexOg)?.ongpro_y}")

        val vpnNativeAds = AdLoader.Builder(
            context.applicationContext,
            idOg
        )
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
            .build()

        vpnNativeAds.withNativeAdOptions(adOptions)
        vpnNativeAds.forNativeAd {
            adBase.appAdDataOg = it
            it.setOnPaidEventListener { adValue ->
                KLog.e("TBA","home-----setOnPaidEventListener")
                it.responseInfo?.let { it1 ->
                    OnlineOkHttpUtils.postAdEvent(adValue,
                        it1,ogDetailBean,"native", "ongpro_n_home")
                }
                //重新缓存
                AdBase.getHomeInstance().advertisementLoadingOg(context)
            }
        }
        vpnNativeAds.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                adBase.isLoadingOg = false
                adBase.appAdDataOg = null
                KLog.d(logTagOg, "home---加载vpn原生加载失败: $error")

                if (adBase.adIndexOg < adData.ongpro_n_home.size - 1) {
                    adBase.adIndexOg++
                    loadHomeAdvertisementOg(context,adData)
                }else{
                    adBase.adIndexOg = 0
                }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                KLog.d(logTagOg, "home---加载vpn原生广告成功")
                adBase.loadTimeOg = Date().time
                adBase.isLoadingOg = false
                adBase.adIndexOg = 0
            }

            override fun onAdOpened() {
                super.onAdOpened()
                KLog.d(logTagOg, "home---点击vpn原生广告")
                recordNumberOfAdClickOg()
            }
        }).build().loadAd(AdRequest.Builder().build())
    }

    /**
     * 设置展示vpn原生广告
     */
    fun setDisplayHomeNativeAdOg(activity: AppCompatActivity, binding: ActivityMainBinding) {
        activity.runOnUiThread {
            adBase.appAdDataOg?.let { adData ->
                if (adData is NativeAd && !adBase.whetherToShowOg && binding.sidebarShowsOg!=true && activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
                    if (activity.isDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                        adData.destroy()
                        return@let
                    }
                    val adView = activity.layoutInflater.inflate(R.layout.layout_main_native_og, null) as NativeAdView
                    // 对应原生组件
                    setCorrespondingNativeComponentOg(adData, adView)
                    binding.ogAdFrame.apply {
                        removeAllViews()
                        addView(adView)
                    }
                    binding.vpnAdOg = true
                    recordNumberOfAdDisplaysOg()
                    adBase.whetherToShowOg = true
                    App.nativeAdRefreshOg = false
                    adBase.appAdDataOg = null
                    KLog.d(logTagOg, "home--原生广告--展示")
                   ogDetailBean= OnlineGameUtils.afterLoadLinkSettingsOg(ogDetailBean)

                }
            }
        }
    }

    private fun setCorrespondingNativeComponentOg(nativeAd: NativeAd, adView: NativeAdView) {
        adView.mediaView = adView.findViewById(R.id.ad_media)
        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)

        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.mediaContent?.let {
            adView.mediaView?.apply { setImageScaleType(ImageView.ScaleType.CENTER_CROP) }
                ?.setMediaContent(it)
        }
        adView.mediaView?.clipToOutline=true
        adView.mediaView?.outlineProvider= RoundCornerOutlineProvider(8f)
        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }
        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as TextView).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(
                nativeAd.icon?.drawable
            )
            adView.iconView?.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)
    }
}