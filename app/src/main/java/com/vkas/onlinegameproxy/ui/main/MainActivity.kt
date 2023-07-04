package com.vkas.onlinegameproxy.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.preference.PreferenceDataStore
import com.airbnb.lottie.LottieAnimationView
import com.blankj.utilcode.util.LogUtils
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key
import com.github.shadowsocks.utils.StartService
import com.google.gson.reflect.TypeToken
import com.lsxiao.apollo.core.annotations.Receive
import com.vkas.onlinegameproxy.R
import com.vkas.onlinegameproxy.ad.OgLoadConnectAd
import com.vkas.onlinegameproxy.ad.OgLoadHomeAd
import com.vkas.onlinegameproxy.app.App
import com.vkas.onlinegameproxy.app.App.Companion.mmkvOg
import com.vkas.onlinegameproxy.base.AdBase
import com.vkas.onlinegameproxy.base.BaseActivityNew
import com.vkas.onlinegameproxy.bean.OgVpnBean
import com.vkas.onlinegameproxy.bean.OpRemoteBean
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.key.Constant.logTagOg
import com.vkas.onlinegameproxy.ui.list.ListActivity
import com.vkas.onlinegameproxy.ui.result.ResultActivity
import com.vkas.onlinegameproxy.ui.web.WebActivity
import com.vkas.onlinegameproxy.utils.*
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.getFlagThroughCountryEc
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.isThresholdReached
import com.vkas.onlinegameproxy.widget.SlidingMenu
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.net.JsonUtil.toJson
import com.xuexiang.xutil.net.NetworkUtils.isNetworkAvailable
import com.xuexiang.xutil.tip.ToastUtils
import kotlinx.coroutines.*

class MainActivity : BaseActivityNew(),
    ShadowsocksConnection.Callback,
    OnPreferenceDataStoreChangeListener, LifecycleObserver, View.OnClickListener {


    // 跳转结果页
    private var liveJumpResultsPage = MutableLiveData<Bundle>()
    private val connection = ShadowsocksConnection(true)

    // 是否返回刷新服务器
    var whetherRefreshServer = false
    private var jobNativeAdsOg: Job? = null
    private var jobStartOg: Job? = null

    val onlineConfig: OpRemoteBean = OnlineGameUtils.getLocalVpnBootData()

    val model by viewModels<MainViewModel>()
    private val txtTimerOg: TextView by bindView(R.id.txt_timer_og)


    private val proList: ProgressBar by bindView(R.id.pro_list)
    private val slMain: SlidingMenu by bindView(R.id.sl_main)
    private val txtCountry: TextView by bindView(R.id.txt_country)
    private val imgCountry: ImageView by bindView(R.id.img_country)
    private val imgState: ImageView by bindView(R.id.img_state)
    private val lavViewOg: LottieAnimationView by bindView(R.id.lav_view_og)
    private val lavViewGu: LottieAnimationView by bindView(R.id.lav_view_gu)
    private val viewGuideMask: View by bindView(R.id.view_guide_mask)
    private val ogAdFrame: FrameLayout by bindView(R.id.og_ad_frame)
    private val imgOgAdFrame: ImageView by bindView(R.id.img_og_ad_frame)
    private val imgNav: ImageView by bindView(R.id.img_nav)
    private val flConnect: FrameLayout by bindView(R.id.fl_connect)
    private val linearLayout3: LinearLayout by bindView(R.id.linearLayout3)
    private lateinit var inHomeNavigation: View

    private lateinit var constraintLayout: ConstraintLayout

    private lateinit var tvPrivacyPolicyPt: TextView
    private lateinit var tvSharePt: TextView
    private lateinit var tvUpgrade: TextView


    companion object {
        var stateListener: ((BaseService.State) -> Unit)? = null
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_main
    }

    @Receive(Constant.TIMER_OG_DATA)
    fun apolloTimerOgData(it: String) {
        txtTimerOg.text = it

    }

    @Receive(Constant.NOT_CONNECTED_OG_RETURN)
    fun apolloNotConnectedOgReturn(it: OgVpnBean) {
        model.updateSkServer(it, false)

    }

    @Receive(Constant.CONNECTED_OG_RETURN)
    fun apolloConnectedOgReturn(it: OgVpnBean) {
        model.updateSkServer(it, true)
    }

    @Receive(Constant.PLUG_OG_ADVERTISEMENT_SHOW)
    fun apolloPlugOgAdvertisementShow(it: Boolean) {
        AdBase.getConnectInstance().advertisementLoadingOg(this@MainActivity)
        model.connectOrDisconnectOg(it, this)
    }

    override fun initView() {
        super.initView()
        inHomeNavigation = findViewById(R.id.in_home_navigation)
        constraintLayout = inHomeNavigation.findViewById(R.id.constraintLayout)
        tvPrivacyPolicyPt = inHomeNavigation.findViewById(R.id.tv_privacy_policy_pt)
        tvSharePt = inHomeNavigation.findViewById(R.id.tv_share_pt)
        tvUpgrade = inHomeNavigation.findViewById(R.id.tv_upgrade)
    }

    override fun initData() {
        super.initData()
        MmkvUtils.set(Constant.SLIDING, true)
        vpnAdOgFun(false)
        vpnStateFun(0)
        homeGuideOgFun(false)
        if(model.isLegalIpAddress(this)){return}
        slMain.setOnClickListener(this)
        imgNav.setOnClickListener(this)
        flConnect.setOnClickListener(this)
        linearLayout3.setOnClickListener(this)
        viewGuideMask.setOnClickListener(this)
        lavViewGu.setOnClickListener(this)

        constraintLayout.setOnClickListener(this)
        tvPrivacyPolicyPt.setOnClickListener(this)
        tvSharePt.setOnClickListener(this)
        tvUpgrade.setOnClickListener(this)

        // 设置状态
        changeState(BaseService.State.Idle, animate = false)

        // 连接服务
        connection.connect(this, this)

        // 注册数据改变监听
        DataStore.publicStore.registerChangeListener(this)

        // 初始化服务数据
        if (OgTimerThread.isStopThread) {
            model.initializeServerData()
        } else {
            val serviceData = mmkvOg.decodeString("currentServerData", "").toString()
            val currentServerData: OgVpnBean =
                JsonUtil.fromJson(serviceData, object : TypeToken<OgVpnBean?>() {}.type)
            model.setFastInformation(currentServerData, txtCountry, imgCountry)
        }

        AdBase.getHomeInstance().whetherToShowOg = false

        // 初始化主页广告
        initHomeAd()

        // 显示VPN指南
        showVpnGuide()
        if (onlineConfig.online_start == "1") {
            judgeVpnScheme()
        }
        // 跳转结果页
        jumpResultsPageData()
        setServiceData()
        vpnStateLiveFun()
        vpnUiLiveFun()
    }

    private fun vpnStateLiveFun() {
        model.vpnStateLive.observe(this, {
            vpnStateFun(it)
        })
    }

    private fun vpnUiLiveFun() {
        model.vpnUiLive.observe(this, {
            vpnUiChanges(it)
        })
    }

    fun vpnAdOgFun(vpnAdOg: Boolean) {
        if (vpnAdOg) {
            ogAdFrame.visibility = View.VISIBLE
        } else {
            ogAdFrame.visibility = View.GONE
        }
        if (vpnAdOg) {
            imgOgAdFrame.visibility = View.GONE
        } else {
            imgOgAdFrame.visibility = View.VISIBLE
        }
    }


    fun homeGuideOgFun(homeGuideOg: Boolean) {
        when (homeGuideOg) {
            true -> {
                viewGuideMask.visibility = View.VISIBLE
                lavViewGu.visibility = View.VISIBLE
                lavViewOg.visibility = View.GONE
            }
            false -> {
                viewGuideMask.visibility = View.GONE
                lavViewGu.visibility = View.GONE
                lavViewOg.visibility = View.VISIBLE

            }
        }
    }

    private fun initHomeAd() {
        vpnAdOgFun(false)
        jobNativeAdsOg = lifecycleScope.launch {
            while (isActive) {
                OgLoadHomeAd.setDisplayHomeNativeAdOg(
                    this@MainActivity, ogAdFrame,
                    imgOgAdFrame
                )
                if (AdBase.getHomeInstance().whetherToShowOg) {
                    jobNativeAdsOg?.cancel()
                    jobNativeAdsOg = null
                }
                delay(1000L)
            }
        }
    }

    private fun jumpResultsPageData() {
        liveJumpResultsPage.observe(this, {
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                delay(300L)
                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    val intent = Intent(this@MainActivity, ResultActivity::class.java)
                    intent.putExtras(it)
                    startActivityForResult(intent, 0x11, it)
                }
            }
        })
        model.liveJumpResultsPage.observe(this, {
            liveJumpResultsPage.postValue(it)
        })
    }

    private fun setServiceData() {
        model.liveInitializeServerData.observe(this, {
            model.setFastInformation(it, txtCountry, imgCountry)
        })
        model.liveUpdateServerData.observe(this, {
            whetherRefreshServer = true
            connect.launch(null)
        })
        model.liveNoUpdateServerData.observe(this, {
            whetherRefreshServer = false
            model.setFastInformation(it, txtCountry, imgCountry)
            connect.launch(null)
        })
    }


    fun linkService() {
        lifecycleScope.launch {
            if (MainFun.vpnState != 1 && !viewGuideMask.isVisible) {
                if (OnlineGameUtils.deliverServerTransitions()) {
                    proList.visibility = View.GONE
                    connect.launch(null)
                } else {
                    proList.visibility = View.VISIBLE
                    delay(2000)
                    proList.visibility = View.GONE
                }
            }
        }
    }


    private val connect = registerForActivityResult(StartService()) {
        homeGuideOgFun(false)
        lifecycleScope.launch(Dispatchers.IO) {
            OnlineGameUtils.getIpInformation()
        }
        if (it) {
            ToastUtils.toast(R.string.no_permissions)
        } else {
            if (isNetworkAvailable()) {
                startVpn()
            } else {
                ToastUtils.toast(getString(R.string.check_your_network), 3000)
            }
        }
    }

    /**
     * 启动VPN
     */
    private fun startVpn() {
        if(model.isLegalIpAddress(this)){return}
        MainFun.statusAtTheTimeOfClick = model.getCurrentStatus()
        val vpnState = 1
        vpnStateFun(vpnState)
        changeOfVpnStatus()
        jobStartOg = lifecycleScope.launch {
            delay(2000L)
            model.connectVpn()
            App.isAppOpenSameDayOg()
            if (model.whetherToImplementPlanA) {
                AdBase.getConnectInstance().advertisementLoadingOg(this@MainActivity)
                AdBase.getResultInstance().advertisementLoadingOg(this@MainActivity)
            }
            try {
                withTimeout(10000L) {
                    delay(1000)
                    while (jobStartOg?.isActive == true) {
                        val showState =
                            OgLoadConnectAd.displayConnectAdvertisementOg(this@MainActivity)
                        when (showState) {
                            "22" -> {
                                jobStartOg?.cancel()
                                jobStartOg = null
                            }
                            "00" -> {
                                jobStartOg?.cancel()
                                jobStartOg = null
                                model.connectOrDisconnectOg(false, this@MainActivity)
                            }
                        }
                        delay(1000L)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                KLogUtils.d("connect---插屏超时")
                model.connectOrDisconnectOg(false, this@MainActivity)
            }
        }
    }


    private fun changeState(
        state: BaseService.State,
        animate: Boolean = true
    ) {
        model.state = state
        connectionStatusJudgment(state.name)
        stateListener?.invoke(state)
    }

    /**
     * 连接状态判断
     */
    private fun connectionStatusJudgment(state: String) {
        KLogUtils.e("connectionStatusJudgment=${state}")
        if (MainFun.statusAtTheTimeOfClick == BaseService.State.Stopped.name && state == BaseService.State.Stopped.name) {
            //vpn连接失败
            KLogUtils.d("vpn连接失败")
            ToastUtils.toast(getString(R.string.connected_failed), 3000)
        }
        when (state) {
            "Connected" -> {
                // 连接成功
                connectionServerSuccessful()
            }
            "Stopped" -> {
                disconnectServerSuccessful()
            }
        }
    }

    /**
     * 连接服务器成功
     */
    private fun connectionServerSuccessful() {
        App.isVpnGlobalLink = true
        LogUtils.e(logTagOg, "连接服务器成功")
        MainFun.getHeartbeatReportedConnect(App.isVpnGlobalLink, this)
    }

    /**
     * 断开服务器
     */
    private fun disconnectServerSuccessful() {
        App.isVpnGlobalLink = false
        MainFun.getHeartbeatReportedDisConnect()
    }

    /**
     * vpn状态变化
     * 是否连接
     */
    private fun changeOfVpnStatus() {
        when (MainFun.vpnState) {
            0 -> {
                imgState.setImageResource(R.drawable.ic_main_connect)
                txtTimerOg.text = getString(R.string._00_00_00)
                txtTimerOg.setTextColor(getColor(R.color.vpn_color))
                OgTimerThread.endTiming()
                lavViewOg.pauseAnimation()
                lavViewOg.visibility = View.GONE
            }
            1 -> {
                imgState.visibility = View.GONE
                lavViewOg.visibility = View.VISIBLE
                lavViewOg.playAnimation()
            }
            2 -> {
                imgState.setImageResource(R.drawable.ic_main_disconnect)
                txtTimerOg.setTextColor(getColor(R.color.vpn_success))
                OgTimerThread.startTiming()
                lavViewOg.pauseAnimation()
                lavViewOg.visibility = View.GONE
            }
        }
    }

    fun vpnStateFun(vpnStateValue: Int) {
        MainFun.vpnState = vpnStateValue
        when (MainFun.vpnState) {
            0 -> {
                imgState.visibility = View.VISIBLE
                lavViewOg.visibility = View.GONE
            }
            1 -> {
                imgState.visibility = View.GONE
                lavViewOg.visibility = View.VISIBLE
            }
            2 -> {
                imgState.visibility = View.VISIBLE
                lavViewOg.visibility = View.GONE
            }
        }
    }

    private fun showVpnGuide() {
        lifecycleScope.launch {
            delay(300)
            if (model.state.name != "Connected") {
                homeGuideOgFun(true)
                lavViewGu.playAnimation()
            } else {
                homeGuideOgFun(false)
                lavViewGu.pauseAnimation()
            }
        }
    }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        changeState(state)
        Log.e("Vpn", "stateChanged===${state.name}")
        model.state = state
        if (model.whetherItIsConnected()) {
            if (!model.whetherToImplementPlanA) {
                KLogUtils.e("清除所有广告重新加载")
                model.clearAllAdsReload(this)
//                model.whetherToImplementPlanA = true
            }
        }
    }

    override fun onServiceConnected(service: IShadowsocksService) {
        val state = BaseService.State.values()[service.state]
        Log.e("Vpn", "onServiceConnected===${state.name}")
        model.state = state
        if (model.whetherItIsConnected()) {
            vpnUiChanges(2)
        } else {
            vpnUiChanges(0)
        }
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        when (key) {
            Key.serviceMode -> {
                connection.disconnect(this)
                connection.connect(this, this)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connection.bandwidthTimeout = 500
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(300)
            if (lifecycle.currentState != Lifecycle.State.RESUMED) {
                return@launch
            }

            if (App.nativeAdRefreshOg) {
                //A方案热启动
                if (onlineConfig.online_start == "2") {
                    judgeVpnScheme()
                }
                changeOfVpnStatus()
                AdBase.getHomeInstance().whetherToShowOg = false
                if (AdBase.getHomeInstance().appAdDataOg != null) {
                    OgLoadHomeAd.setDisplayHomeNativeAdOg(
                        this@MainActivity, ogAdFrame,
                        imgOgAdFrame
                    )
                } else {
                    AdBase.getHomeInstance().advertisementLoadingOg(this@MainActivity)
                    initHomeAd()
                }
            }
            App.whetherHotStart = false
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        connection.bandwidthTimeout = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        DataStore.publicStore.unregisterChangeListener(this)
        connection.disconnect(this)
        jobStartOg?.cancel()
        jobStartOg = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0x11 && whetherRefreshServer) {
            model.setFastInformation(model.afterDisconnectionServerData, txtCountry, imgCountry)
            val serviceData = toJson(model.afterDisconnectionServerData)
            MmkvUtils.set("currentServerData", serviceData)
            model.currentServerData = model.afterDisconnectionServerData
        }
        if (requestCode == 0x11) {
            lifecycleScope.launch {
                delay(300)
                if (lifecycle.currentState != Lifecycle.State.RESUMED) {
                    return@launch
                }
                AdBase.getHomeInstance().whetherToShowOg = false
                if (AdBase.getHomeInstance().appAdDataOg != null) {
                    OgLoadHomeAd.setDisplayHomeNativeAdOg(
                        this@MainActivity, ogAdFrame,
                        imgOgAdFrame
                    )
                } else {
                    AdBase.getHomeInstance().advertisementLoadingOg(this@MainActivity)
                    initHomeAd()
                }
            }
        }
    }



    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.img_nav -> {
                if (MainFun.vpnState != 1 && !viewGuideMask.isVisible) {
                    slMain.open()
                }
            }
            R.id.view_guide_mask -> {
            }
            R.id.linearLayout3 -> {
                model.clickService(this, model.state.name, viewGuideMask, proList)
            }
            R.id.fl_connect -> {
                linkService()
            }
            R.id.lav_view_gu -> {
                if (MainFun.vpnState != 1 && viewGuideMask.isVisible) {
                    connect.launch(null)
                }
            }
            R.id.constraintLayout -> {
            }
            R.id.tv_privacy_policy_pt -> {
                val intent = Intent(this@MainActivity, WebActivity::class.java)
                startActivity(intent)
            }
            R.id.tv_share_pt -> {
                model.toShare(this)
            }
            R.id.tv_upgrade -> {
                model.toUpgrade(this)
            }
        }
    }

    /**
     * 判断Vpn方案
     */
    private fun judgeVpnScheme() {
        if (!model.isItABuyingUser()) {
            //非买量用户直接走A方案
            model.whetherToImplementPlanA = true
            return
        }
        val data = onlineConfig.online_ratio
        if (Utils.isNullOrEmpty(data)) {
            KLogUtils.d("判断Vpn方案---默认")
            vpnCScheme("50")
        } else {
            //C
            model.whetherToImplementPlanA = false
            vpnCScheme(data)
        }
    }

    /**
     * vpn B 方案
     */
    private fun vpnBScheme() {
        lifecycleScope.launch {
            delay(300)
            if (!model.state.canStop) {
                connect.launch(null)
            }
        }
    }

    /**
     * vpn C 方案
     * 概率
     */
    private fun vpnCScheme(mProbability: String) {
        val mProbabilityInt = mProbability.toIntOrNull()
        if (mProbabilityInt == null) {
            model.whetherToImplementPlanA = true
        } else {
            val random = (0..100).shuffled().last()
            when {
                random <= mProbabilityInt -> {
                    //B
                    KLogUtils.d("随机落在B方案")
                    vpnBScheme() //20，代表20%为B用户；80%为A用户
                }
                else -> {
                    //A
                    KLogUtils.d("随机落在A方案")
                    model.whetherToImplementPlanA = true
                }
            }
        }
    }

    /**
     * vpn UI改变
     */
    private fun vpnUiChanges(vpnStateValue: Int) {
        vpnStateFun(vpnStateValue)
        changeOfVpnStatus()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (viewGuideMask.isVisible) {
                homeGuideOgFun(false)
                lavViewGu.pauseAnimation()
            } else {
                if (!(lavViewOg.isAnimating && MainFun.statusAtTheTimeOfClick == BaseService.State.Stopped.name)) {
                    finish()
                }
            }
        }
        return true
    }
}