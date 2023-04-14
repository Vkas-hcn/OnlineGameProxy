package com.vkas.onlinegameproxy.ui.start

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.onlinegameproxy.BR
import com.vkas.onlinegameproxy.BuildConfig
import com.vkas.onlinegameproxy.R
import com.vkas.onlinegameproxy.ad.OgLoadOpenAd
import com.vkas.onlinegameproxy.app.App
import com.vkas.onlinegameproxy.base.AdBase
import com.vkas.onlinegameproxy.base.BaseActivity
import com.vkas.onlinegameproxy.base.BaseViewModel
import com.vkas.onlinegameproxy.databinding.ActivityStartBinding
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.key.Constant.logTagOg
import com.vkas.onlinegameproxy.ui.main.MainActivity
import com.vkas.onlinegameproxy.utils.KLog
import com.vkas.onlinegameproxy.utils.MmkvUtils
import com.vkas.onlinegameproxy.utils.OnlineGameUtils
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.isThresholdReached
import com.xuexiang.xui.widget.progress.HorizontalProgressView
import kotlinx.coroutines.*
import java.util.*

class StartActivity : BaseActivity<ActivityStartBinding, BaseViewModel>(),
    HorizontalProgressView.HorizontalProgressUpdateListener {
    companion object {
        var isCurrentPage: Boolean = false
    }

    private var liveJumpHomePage = MutableLiveData<Boolean>()
    private var liveJumpHomePage2 = MutableLiveData<Boolean>()
    private var jobOpenAdsOg: Job? = null

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_start
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
        isCurrentPage = intent.getBooleanExtra(Constant.RETURN_OG_CURRENT_PAGE, false)

    }

    override fun initToolbar() {
        super.initToolbar()
    }

    override fun initData() {
        super.initData()
//        val testDeviceIds = listOf("9CDD654B92424BD2715643A8BFC44CD0")
//        val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
//        MobileAds.setRequestConfiguration(configuration)

        binding.pbStartOg.setProgressViewUpdateListener(this)
        binding.pbStartOg.setProgressDuration(10000)
        binding.pbStartOg.setProgressTextVisibility(true)
        binding.pbStartOg.startProgressAnimation()
        liveEventBusOg()
        lifecycleScope.launch(Dispatchers.IO) {
            OnlineGameUtils.referrer(this@StartActivity)
            OnlineGameUtils.getIpInformation()
        }
        getFirebaseDataOg()
        jumpHomePageData()
    }

    private fun liveEventBusOg() {
        LiveEventBus
            .get(Constant.OPEN_CLOSE_JUMP, Boolean::class.java)
            .observeForever {
                KLog.d(logTagOg, "关闭开屏内容-接收==${this.lifecycle.currentState}")
                if (this.lifecycle.currentState == Lifecycle.State.STARTED) {
                    jumpPage()
                }
            }
    }

    private fun getFirebaseDataOg() {
        if (BuildConfig.DEBUG) {
            preloadedAdvertisement()
//            lifecycleScope.launch {
//                val ips = listOf("192.168.0.1", "8.8.8.8", "114.114.114.114")
//                val fastestIP = findFastestIP(ips)
//                KLog.e("TAG", "Fastest IP: $fastestIP")
//                delay(1500)
//                MmkvUtils.set(
//                    Constant.ADVERTISING_OG_DATA,
//                    ResourceUtils.readStringFromAssert("elAdDataFireBase.json")
//                )
//            }
            return
        } else {
            preloadedAdvertisement()
            val auth = Firebase.remoteConfig
            auth.fetchAndActivate().addOnSuccessListener {
                MmkvUtils.set(Constant.PROFILE_OG_DATA, auth.getString("ongpro_server"))
                MmkvUtils.set(Constant.PROFILE_OG_DATA_FAST, auth.getString("ongpro_smart"))
                MmkvUtils.set(Constant.AROUND_OG_FLOW_DATA, auth.getString("ongproAroundFlow_Data"))
                MmkvUtils.set(Constant.ADVERTISING_OG_DATA, auth.getString("ongpro_ad"))
                MmkvUtils.set(Constant.ONLINE_CONFIG, auth.getString(Constant.ONLINE_CONFIG))

            }
        }
    }

    override fun initViewObservable() {
        super.initViewObservable()
    }

    private fun jumpHomePageData() {
        liveJumpHomePage2.observe(this, {
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                KLog.e("TAG", "isBackDataOg==${App.isBackDataOg}")
                delay(300)
                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    jumpPage()
                }
            }
        })
        liveJumpHomePage.observe(this, {
            liveJumpHomePage2.postValue(true)
        })
    }

    /**
     * 跳转页面
     */
    private fun jumpPage() {
        // 不是后台切回来的跳转，是后台切回来的直接finish启动页
        if (!isCurrentPage) {
            val intent = Intent(this@StartActivity, MainActivity::class.java)
            startActivity(intent)
        }
        finish()
    }

    /**
     * 加载广告
     */
    private fun loadAdvertisement() {
        // 开屏
        AdBase.getOpenInstance().adIndexOg = 0
        AdBase.getOpenInstance().advertisementLoadingOg(this)
        rotationDisplayOpeningAdOg()
        // 首页原生
        AdBase.getHomeInstance().adIndexOg = 0
        AdBase.getHomeInstance().advertisementLoadingOg(this)
        // 结果页原生
        AdBase.getResultInstance().adIndexOg = 0
        AdBase.getResultInstance().advertisementLoadingOg(this)
        // 连接插屏
        AdBase.getConnectInstance().adIndexOg = 0
        AdBase.getConnectInstance().advertisementLoadingOg(this)
        // 服务器页插屏
        AdBase.getBackInstance().adIndexOg = 0
        AdBase.getBackInstance().advertisementLoadingOg(this)
        // 服务器页原生
        AdBase.getListInstance().adIndexOg = 0
        AdBase.getListInstance().advertisementLoadingOg(this)
    }

    /**
     * 轮训展示开屏广告
     */
    private fun rotationDisplayOpeningAdOg() {
        jobOpenAdsOg = lifecycleScope.launch {
            try {
                withTimeout(10000L) {
                    delay(1000L)
                    while (isActive) {
                        val showState = OgLoadOpenAd
                            .displayOpenAdvertisementOg(this@StartActivity)
                        if (showState) {
                            jobOpenAdsOg?.cancel()
                            jobOpenAdsOg = null
                        }
                        delay(1000L)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                KLog.e("TimeoutCancellationException I'm sleeping $e")
                jumpPage()
            }
        }
    }

    /**
     * 预加载广告
     */
    private fun preloadedAdvertisement() {
        App.isAppOpenSameDayOg()
        if (isThresholdReached()) {
            KLog.d(logTagOg, "广告达到上线")
            lifecycleScope.launch {
                delay(2000L)
                liveJumpHomePage.postValue(true)
            }
        } else {
            loadAdvertisement()
        }
    }

    override fun onHorizontalProgressStart(view: View?) {
    }

    override fun onHorizontalProgressUpdate(view: View?, progress: Float) {
    }

    override fun onHorizontalProgressFinished(view: View?) {
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return keyCode == KeyEvent.KEYCODE_BACK
    }
}