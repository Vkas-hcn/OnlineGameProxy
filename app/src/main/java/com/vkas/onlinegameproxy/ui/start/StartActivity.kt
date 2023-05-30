package com.vkas.onlinegameproxy.ui.start

import android.content.Intent
import android.view.KeyEvent
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.onlinegameproxy.BuildConfig
import com.vkas.onlinegameproxy.R
import com.vkas.onlinegameproxy.ad.OgLoadOpenAd
import com.vkas.onlinegameproxy.app.App
import com.vkas.onlinegameproxy.base.AdBase
import com.vkas.onlinegameproxy.base.BaseActivityNew
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.key.Constant.logTagOg
import com.vkas.onlinegameproxy.ui.main.MainActivity
import com.vkas.onlinegameproxy.utils.*
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.isThresholdReached
import com.vkas.onlinegameproxy.widget.HorizontalProgressViewOg
import com.xuexiang.xui.widget.progress.HorizontalProgressView
import com.xuexiang.xutil.net.NetworkUtils
import kotlinx.coroutines.*
import java.util.*

class StartActivity : BaseActivityNew(),
    HorizontalProgressView.HorizontalProgressUpdateListener {
    val model by viewModels<StartViewModel>()
    var isCurrentPage: Boolean = false
    private var liveJumpHomePage = MutableLiveData<Boolean>()
    private var liveJumpHomePage2 = MutableLiveData<Boolean>()
    private var jobOpenAdsOg: Job? = null
    private val horizontalProgressViewOg: HorizontalProgressViewOg by bindView(R.id.pb_start_og)

    override fun getLayoutId(): Int {
        return R.layout.activity_start
    }

    override fun initView() {
        isCurrentPage = intent.getBooleanExtra(Constant.RETURN_OG_CURRENT_PAGE, false)

    }

    override fun initData() {
        // 初始化数据
        val fastData = OnlineGameUtils.sendResultDecoding("123456")

//        val data = OnlineTbaUtils.install(this)
//        KLog.e("TAG","data=======$data")
//        val testDeviceIds = listOf("9CDD654B92424BD2715643A8BFC44CD0")
//        val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
//        MobileAds.setRequestConfiguration(configuration)

        horizontalProgressViewOg.setProgressViewUpdateListener(this)
        horizontalProgressViewOg.setProgressDuration(10000)
        horizontalProgressViewOg.setProgressTextVisibility(true)
        horizontalProgressViewOg.startProgressAnimation()
        liveEventBusOg()
        lifecycleScope.launch(Dispatchers.IO) {
            if (!NetworkUtils.isNetworkAvailable()) {
                return@launch
            }
            runBlocking {
                OnlineOkHttpUtils.getDeliverData()
                OnlineTbaUtils.obtainGoogleAdvertisingId(this@StartActivity)
                OnlineTbaUtils.obtainIpAddress()
            }
            OnlineGameUtils.referrer(this@StartActivity)
            OnlineOkHttpUtils.postSessionEvent()
            OnlineOkHttpUtils.getBlacklistData()
        }


        getFirebaseDataOg()
        jumpHomePageData()
    }

    override fun setupListeners() {
        // 设置监听器
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