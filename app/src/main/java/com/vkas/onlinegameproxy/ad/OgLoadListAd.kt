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
import com.vkas.onlinegameproxy.databinding.ActivityServiceListOgBinding
import com.vkas.onlinegameproxy.key.Constant.logTagOg
import com.vkas.onlinegameproxy.utils.KLog
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.recordNumberOfAdClickOg
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.takeSortedAdIDOg
import java.util.*
import com.vkas.onlinegameproxy.R
import com.vkas.onlinegameproxy.app.App
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.recordNumberOfAdDisplaysOg
import com.vkas.onlinegameproxy.utils.RoundCornerOutlineProvider

object OgLoadListAd {
    private val adBase = AdBase.getListInstance()

    /**
     * 加载list原生广告
     */
    fun loadListAdvertisementOg(context: Context, adData: OgAdBean) {
        val id = takeSortedAdIDOg(adBase.adIndexOg, adData.ongpro_n_ser)
        KLog.d(logTagOg, "list--原生广告id=$id;权重=${adData.ongpro_n_ser.getOrNull(adBase.adIndexOg)?.ongpro_y}")

        val vpnNativeAds = AdLoader.Builder(
            context.applicationContext,
            id
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
                KLog.d(logTagOg, "list--加载vpn原生加载失败: $error")

                if (adBase.adIndexOg < adData.ongpro_n_ser.size - 1) {
                    adBase.adIndexOg++
                    loadListAdvertisementOg(context,adData)
                }else{
                    adBase.adIndexOg = 0
                }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                KLog.d(logTagOg, "list--加载vpn原生广告成功")
                adBase.loadTimeOg = Date().time
                adBase.isLoadingOg = false
                adBase.adIndexOg = 0
            }

            override fun onAdOpened() {
                super.onAdOpened()
                KLog.d(logTagOg, "list--点击vpn原生广告")
                recordNumberOfAdClickOg()
            }
        }).build().loadAd(AdRequest.Builder().build())
    }

    /**
     * 设置展示list原生广告
     */
    fun setDisplayListNativeAdOg(activity: AppCompatActivity, binding: ActivityServiceListOgBinding) {
        activity.runOnUiThread {
            adBase.appAdDataOg?.let { adData ->
                if (adData is NativeAd && !adBase.whetherToShowOg && activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
                    if (activity.isDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                        adData.destroy()
                        return@let
                    }
                    val adView = activity.layoutInflater.inflate(R.layout.layout_list_native_og, null) as NativeAdView
                    // 对应原生组件
                    setCorrespondingNativeComponentOg(adData, adView)
                    binding.ogAdFrame.apply {
                        removeAllViews()
                        addView(adView)
                    }
                    binding.listAdOg = true
                    recordNumberOfAdDisplaysOg()
                    adBase.whetherToShowOg = true
                    App.nativeAdRefreshOg = false
                    adBase.appAdDataOg = null
                    KLog.d(logTagOg, "list-原生广告--展示")
                    //重新缓存
                    AdBase.getListInstance().advertisementLoadingOg(activity)
                }
            }
        }
    }

    private fun setCorrespondingNativeComponentOg(nativeAd: NativeAd, adView: NativeAdView) {
        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }
        (adView.headlineView as TextView).text = nativeAd.headline

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