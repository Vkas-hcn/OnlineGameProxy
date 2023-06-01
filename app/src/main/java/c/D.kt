package c

import android.content.Intent
import android.view.KeyEvent
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.AppUtils
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.lsxiao.apollo.core.Apollo
import com.lsxiao.apollo.core.annotations.Receive
import op.asd.BuildConfig
import op.asd.R
import op.asd.ad.OgLoadOpenAd
import b.C
import op.asd.base.AdBase
import op.asd.base.BaseActivityNew
import op.asd.key.Constant
import op.asd.key.Constant.logTagOg
import d.E
import op.asd.utils.*
import op.asd.utils.OnlineGameUtils.isThresholdReached
import a.B
import com.xuexiang.xui.widget.progress.HorizontalProgressView
import com.xuexiang.xutil.net.NetworkUtils
import kotlinx.coroutines.*
import java.util.*

class D : BaseActivityNew(),
    HorizontalProgressView.HorizontalProgressUpdateListener {
    val model by viewModels<P>()
    var isCurrentPage: Boolean = false
    private var liveJumpHomePage = MutableLiveData<Boolean>()
    private var liveJumpHomePage2 = MutableLiveData<Boolean>()
    private var jobOpenAdsOg: Job? = null
    private val horizontalProgressViewOg: B by bindView(R.id.pb_start_og)

    override fun getLayoutId(): Int {
        return R.layout.activity_start
    }

    override fun initView() {
        isCurrentPage = intent.getBooleanExtra(Constant.RETURN_OG_CURRENT_PAGE, false)

    }

    override fun initData() {
        horizontalProgressViewOg.setProgressViewUpdateListener(this)
        horizontalProgressViewOg.setProgressDuration(10000)
        horizontalProgressViewOg.setProgressTextVisibility(true)
        horizontalProgressViewOg.startProgressAnimation()
        lifecycleScope.launch(Dispatchers.IO) {
            if (!NetworkUtils.isNetworkAvailable()) {
                return@launch
            }
            runBlocking {
                OnlineOkHttpUtils.getDeliverData()
                OnlineTbaUtils.obtainGoogleAdvertisingId(this@D)
                OnlineTbaUtils.obtainIpAddress()
            }
            OnlineGameUtils.referrer(this@D)
            OnlineOkHttpUtils.postSessionEvent()
            OnlineOkHttpUtils.getBlacklistData()
        }


        getFirebaseDataOg()
        jumpHomePageData()
    }

    override fun setupListeners() {
        // 设置监听器
    }

    @Receive(Constant.OPEN_CLOSE_JUMP)
     fun liveEventBusOg(it:Boolean) {
        KLogUtils.d("关闭开屏内容-接收==${this.lifecycle.currentState}")
        if (this.lifecycle.currentState == Lifecycle.State.STARTED) {
            jumpPage()
        }
    }

    private fun getFirebaseDataOg() {
        if (BuildConfig.DEBUG) {
            preloadedAdvertisement()
//            lifecycleScope.launch {
//                val ips = listOf("192.168.0.1", "8.8.8.8", "114.114.114.114")
//                val fastestIP = findFastestIP(ips)
//                KLogUtils.e("TAG", "Fastest IP: $fastestIP")
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
            val intent = Intent(this@D, E::class.java)
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
                            .displayOpenAdvertisementOg(this@D)
                        if (showState) {
                            jobOpenAdsOg?.cancel()
                            jobOpenAdsOg = null
                        }
                        delay(1000L)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                KLogUtils.e("TimeoutCancellationException I'm sleeping $e")
                jumpPage()
            }
        }
    }

    /**
     * 预加载广告
     */
    private fun preloadedAdvertisement() {
        C.isAppOpenSameDayOg()
        if (isThresholdReached()) {
            KLogUtils.d( "广告达到上线")
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