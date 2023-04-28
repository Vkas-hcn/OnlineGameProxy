package com.vkas.onlinegameproxy.ui.main

import android.animation.AnimatorSet
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.RemoteException
import android.view.KeyEvent
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceDataStore
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key
import com.github.shadowsocks.utils.StartService
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.onlinegameproxy.BR
import com.vkas.onlinegameproxy.R
import com.vkas.onlinegameproxy.ad.OgLoadConnectAd
import com.vkas.onlinegameproxy.ad.OgLoadHomeAd
import com.vkas.onlinegameproxy.app.App
import com.vkas.onlinegameproxy.app.App.Companion.mmkvOg
import com.vkas.onlinegameproxy.base.AdBase
import com.vkas.onlinegameproxy.base.BaseActivity
import com.vkas.onlinegameproxy.bean.OgVpnBean
import com.vkas.onlinegameproxy.bean.OpRemoteBean
import com.vkas.onlinegameproxy.databinding.ActivityMainBinding
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.key.Constant.logTagOg
import com.vkas.onlinegameproxy.ui.list.ListActivity
import com.vkas.onlinegameproxy.ui.result.ResultActivity
import com.vkas.onlinegameproxy.ui.web.WebActivity
import com.vkas.onlinegameproxy.utils.*
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.getFlagThroughCountryEc
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.isThresholdReached
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.net.JsonUtil.toJson
import com.xuexiang.xutil.net.NetworkUtils.isNetworkAvailable
import com.xuexiang.xutil.tip.ToastUtils
import kotlinx.coroutines.*

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>(),
    ShadowsocksConnection.Callback,
    OnPreferenceDataStoreChangeListener, LifecycleObserver {
    var state = BaseService.State.Idle
    private var jobHeart: Job? = null

    //重复点击
    var repeatClick = false
    private var jobRepeatClick: Job? = null

    // 跳转结果页
    private var liveJumpResultsPage = MutableLiveData<Bundle>()
    private val connection = ShadowsocksConnection(true)

    // 是否返回刷新服务器
    var whetherRefreshServer = false
    private var jobNativeAdsOg: Job? = null
    private var jobStartOg: Job? = null

    //连接vpn job
    private var jobVpnOg: Job? = null

    //关闭插屏状态
    private var turnOffTouchScreenStatus: Boolean? = null

    //当前执行连接操作
    private var performConnectionOperations: Boolean = false

    //是否点击连接
    private var clickToConnect: Boolean = false

    //是否执行A方案
    private var whetherToImplementPlanA = false

    //是否执行B方案
    private var whetherToImplementPlanB = false
    val onlineConfig: OpRemoteBean = OnlineGameUtils.getLocalVpnBootData()

    companion object {
        var stateListener: ((BaseService.State) -> Unit)? = null
    }

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_main
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
    }

    override fun initToolbar() {
        super.initToolbar()
        binding.presenter = OgClick()
        MmkvUtils.set(Constant.SLIDING,true)
        liveEventBusReceive()
    }

    private fun liveEventBusReceive() {
        LiveEventBus
            .get(Constant.TIMER_OG_DATA, String::class.java)
            .observeForever {
                binding.txtTimerOg.text = it
            }
        LiveEventBus
            .get(Constant.STOP_VPN_CONNECTION, Boolean::class.java)
            .observeForever {
                if (state.canStop) {
                    performConnectionOperations = false
                    Core.stopService()
                }
            }

        //更新服务器(未连接)
        LiveEventBus
            .get(Constant.NOT_CONNECTED_OG_RETURN, OgVpnBean::class.java)
            .observeForever {
                viewModel.updateSkServer(it, false)
            }
        //更新服务器(已连接)
        LiveEventBus
            .get(Constant.CONNECTED_OG_RETURN, OgVpnBean::class.java)
            .observeForever {
                viewModel.updateSkServer(it, true)
            }
        //插屏关闭后跳转
        LiveEventBus
            .get(Constant.PLUG_OG_ADVERTISEMENT_SHOW, Boolean::class.java)
            .observeForever {
                KLog.e("state", "插屏关闭接收=${it}")
                //重复点击
                jobRepeatClick = lifecycleScope.launch {
                    if (!repeatClick) {
                        KLog.e("state", "插屏关闭后跳转=${it}")
                        AdBase.getConnectInstance().advertisementLoadingOg(this@MainActivity)
                        connectOrDisconnectOg(it)
                        repeatClick = true
                    }
                    delay(1000)
                    repeatClick = false
                }
            }
    }

    override fun initData() {
        super.initData()
//        if (viewModel.whetherParsingIsIllegalIp()) {
//            viewModel.whetherTheBulletBoxCannotBeUsed(this@MainActivity)
//            return
//        }

        // 设置状态
        changeState(BaseService.State.Idle, animate = false)

        // 连接服务
        connection.connect(this, this)

        // 注册数据改变监听
        DataStore.publicStore.registerChangeListener(this)

        // 初始化服务数据
        if (OgTimerThread.isStopThread) {
            viewModel.initializeServerData()
        } else {
            val serviceData = mmkvOg.decodeString("currentServerData", "").toString()
            val currentServerData: OgVpnBean =
                JsonUtil.fromJson(serviceData, object : TypeToken<OgVpnBean?>() {}.type)
            setFastInformation(currentServerData)
        }

        AdBase.getHomeInstance().whetherToShowOg = false

        // 初始化主页广告
        initHomeAd()

        // 显示VPN指南
        showVpnGuide()
        if (onlineConfig.online_start == "1") {
            judgeVpnScheme()
        }
    }

    private fun initHomeAd() {
        binding.vpnAdOg = false
        jobNativeAdsOg = lifecycleScope.launch {
            while (isActive) {
                OgLoadHomeAd.setDisplayHomeNativeAdOg(this@MainActivity, binding)
                if (AdBase.getHomeInstance().whetherToShowOg) {
                    jobNativeAdsOg?.cancel()
                    jobNativeAdsOg = null
                }
                delay(1000L)
            }
        }
    }

    override fun initViewObservable() {
        super.initViewObservable()
        // 跳转结果页
        jumpResultsPageData()
        setServiceData()
    }

    private fun jumpResultsPageData() {
        liveJumpResultsPage.observe(this, {
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                delay(300L)
                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    startActivityForResult(ResultActivity::class.java, 0x11, it)
                }
            }
        })
        viewModel.liveJumpResultsPage.observe(this, {
            liveJumpResultsPage.postValue(it)
        })
    }

    private fun setServiceData() {
        viewModel.liveInitializeServerData.observe(this, {
            setFastInformation(it)
        })
        viewModel.liveUpdateServerData.observe(this, {
            whetherRefreshServer = true
            connect.launch(null)
        })
        viewModel.liveNoUpdateServerData.observe(this, {
            whetherRefreshServer = false
            setFastInformation(it)
            connect.launch(null)
        })
    }

    inner class OgClick {
        fun linkService() {
            lifecycleScope.launch {
                if (binding.vpnState != 1 && !binding.viewGuideMask.isVisible) {
                    if (OnlineGameUtils.deliverServerTransitions()) {
                        binding.proList.visibility = View.GONE
                        connect.launch(null)

                    } else {
                        binding.proList.visibility = View.VISIBLE
                        delay(2000)
                        binding.proList.visibility = View.GONE
                    }
                }
            }

//            if (binding.vpnState == 0) {
//                UnLimitedUtils.getBuriedPointOg("unlimF_clickv")
//            }
        }
        fun toNav(){
            if (binding.vpnState != 1 && !binding.viewGuideMask.isVisible) {
                binding.slMain.open()
            }
        }
        fun linkServiceGuide() {
            if (binding.vpnState != 1 && binding.viewGuideMask.isVisible) {
                connect.launch(null)
            }
        }

        fun clickService() {
            lifecycleScope.launch {
                if (binding.vpnState != 1 && !binding.viewGuideMask.isVisible) {
                    if (OnlineGameUtils.deliverServerTransitions()) {
                        binding.proList.visibility = View.GONE
                        jumpToServerList()
                    } else {
                        binding.proList.visibility = View.VISIBLE
                        delay(2000)
                        binding.proList.visibility = View.GONE
                    }
                }
            }
        }

        fun openOrCloseMenu() {
            binding.sidebarShowsOg = binding.sidebarShowsOg != true
        }

        fun clickMain() {
            KLog.e("TAG", "binding.sidebarShowsOg===>${binding.sidebarShowsOg}")
            if (binding.sidebarShowsOg == true) {
                binding.sidebarShowsOg = false
            }
        }

        fun clickMainMenu() {

        }

        fun toContactUs() {
            val uri = Uri.parse("mailto:${Constant.MAILBOX_OG_ADDRESS}")
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            runCatching {
                startActivity(intent)
            }.onFailure {
                ToastUtils.toast("Please set up a Mail account")
            }
        }

        fun toPrivacyPolicy() {
            startActivity(WebActivity::class.java)
        }

        fun toShare() {
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(
                Intent.EXTRA_TEXT,
                Constant.SHARE_OG_ADDRESS + this@MainActivity.packageName
            )
            intent.type = "text/plain"
            startActivity(intent)
        }

        fun toUpgrade() {
            viewModel.openInBrowser(
                this@MainActivity,
                Constant.SHARE_OG_ADDRESS + this@MainActivity.packageName
            )
        }

        fun clickHome() {
            if (binding.homeGuideOg == false && !(state.name != "Connected" && binding.vpnState == 1)) {
                binding.sidebarShowsOg = false
            }
        }

        fun clickSetting() {
            if (binding.homeGuideOg == false && !(state.name != "Connected" && binding.vpnState == 1)) {
                binding.sidebarShowsOg = true
            }
        }
        fun toHomeGuideOg(){}
    }

    /**
     * 跳转服务器列表
     */
    fun jumpToServerList() {
        lifecycleScope.launch {
            if (lifecycle.currentState != Lifecycle.State.RESUMED) {
                return@launch
            }
            val bundle = Bundle()
            if (state.name == "Connected") {
                bundle.putBoolean(Constant.WHETHER_OG_CONNECTED, true)
            } else {
                bundle.putBoolean(Constant.WHETHER_OG_CONNECTED, false)
            }
            AdBase.getBackInstance().advertisementLoadingOg(this@MainActivity)
            val serviceData = mmkvOg.decodeString("currentServerData", "").toString()
            bundle.putString(Constant.CURRENT_OG_SERVICE, serviceData)
            startActivity(ListActivity::class.java, bundle)
        }
    }

    /**
     * 设置fast信息
     */
    private fun setFastInformation(elVpnBean: OgVpnBean) {
        MmkvUtils.set(Constant.IP_AFTER_VPN_LINK_OG, elVpnBean.ongpro_ip)
        MmkvUtils.set(Constant.IP_AFTER_VPN_CITY_OG, elVpnBean.ongpro_city)
        if (elVpnBean.og_best == true) {
            binding.txtCountry.text = Constant.FASTER_OG_SERVER
            binding.imgCountry.setImageResource(getFlagThroughCountryEc(Constant.FASTER_OG_SERVER))

        } else {
            binding.txtCountry.text = elVpnBean.ongpro_country.toString()
            binding.imgCountry.setImageResource(getFlagThroughCountryEc(elVpnBean.ongpro_country.toString()))

        }
    }

    private val connect = registerForActivityResult(StartService()) {
        binding.homeGuideOg = false
        lifecycleScope.launch(Dispatchers.IO) {
            OnlineGameUtils.getIpInformation()
        }
        if (it) {
            ToastUtils.toast(R.string.no_permissions)
        } else {
//            EasyConnectUtils.getBuriedPointOg("unlimF_geta")
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
        binding.vpnState = 1
        clickToConnect = true
        changeOfVpnStatus()
        jobStartOg = lifecycleScope.launch {
            App.isAppOpenSameDayOg()
            if (isThresholdReached() || Utils.isNullOrEmpty(OgLoadConnectAd.idOg)) {
                delay(2000L)
                KLog.d(logTagOg, "广告达到上线,或者无广告位")
                val showState =
                    OgLoadConnectAd
                        .displayConnectAdvertisementOg(this@MainActivity)
                if (showState!=2) {
                    connectOrDisconnectOg(false)
                }
                return@launch
            }
            AdBase.getConnectInstance().advertisementLoadingOg(this@MainActivity)
//            AdBase.getResultInstance().advertisementLoadingOg(this@MainActivity)
            try {
                withTimeout(10000L) {
                    delay(2000L)
                    KLog.e(logTagOg, "jobStartOg?.isActive=${jobStartOg?.isActive}")
                        while (jobStartOg?.isActive == true) {
                            val showState =
                                OgLoadConnectAd
                                    .displayConnectAdvertisementOg(this@MainActivity)
                            if (showState == 2) {
                                jobStartOg?.cancel()
                                jobStartOg = null
                            }
                            if (showState == 0) {
                                jobStartOg?.cancel()
                                jobStartOg = null
                                connectOrDisconnectOg(false)
                            }
                            delay(1000L)
                        }

                }
            } catch (e: TimeoutCancellationException) {
                KLog.d(logTagOg, "connect---插屏超时")
                connectOrDisconnectOg(false)
            }
        }
    }


    /**
     * 连接或断开
     * 是否后台关闭（true：后台关闭；false：手动关闭）
     */
    private fun connectOrDisconnectOg(isBackgroundClosed: Boolean) {
        KLog.e("state", "连接或断开")
//        if (viewModel.whetherParsingIsIllegalIp()) {
//            viewModel.whetherTheBulletBoxCannotBeUsed(this@MainActivity)
//            return
//        }
        performConnectionOperations = if (state.canStop) {
            if (!isBackgroundClosed) {
                viewModel.jumpConnectionResultsPage(false)
            }
            if ((lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))) {
                Core.stopService()
            } else {
                binding.vpnState = 2
            }
            false
        } else {
            if ((lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))) {
                Core.startService()
                if (!whetherToImplementPlanA && !whetherToImplementPlanB) {
                    viewModel.clearAllAdsReload(this@MainActivity)
                }
            } else {
                binding.vpnState = 0
            }
            if (!isBackgroundClosed) {
                viewModel.jumpConnectionResultsPage(true)
            }
            true
        }
    }

    private fun changeState(
        state: BaseService.State,
        animate: Boolean = true
    ) {
        this.state = state
        connectionStatusJudgment(state.name)
        stateListener?.invoke(state)
    }

    /**
     * 连接状态判断
     */
    private fun connectionStatusJudgment(state: String) {
        KLog.e("TAG", "connectionStatusJudgment=${state}")
        if (performConnectionOperations && state != "Connected") {
            //vpn连接失败
            KLog.d(logTagOg, "vpn连接失败")
//            EasyConnectUtils.getBuriedPointOg("unlimF_vF")
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
        binding.vpnState = 2
        changeOfVpnStatus()
        whetherToImplementPlanB =true
        App.isVpnGlobalLink = true
        getHeartbeatReportedConnect(App.isVpnGlobalLink)
    }

    /**
     * 断开服务器
     */
    private fun disconnectServerSuccessful() {
        KLog.e("TAG", "断开服务器")
        binding.vpnState = 0
        changeOfVpnStatus()
        App.isVpnGlobalLink = false
        getHeartbeatReportedDisConnect()
    }

    /**
     * vpn状态变化
     * 是否连接
     */
    private fun changeOfVpnStatus() {

        when (binding.vpnState) {
            0 -> {
                binding.imgState.setImageResource(R.drawable.ic_main_connect)
                binding.txtTimerOg.text = getString(R.string._00_00_00)
                binding.txtTimerOg.setTextColor(getColor(R.color.vpn_color))
                OgTimerThread.endTiming()
                binding.lavViewOg.pauseAnimation()
                binding.lavViewOg.visibility = View.GONE
            }
            1 -> {
                binding.imgState.visibility = View.GONE
                binding.lavViewOg.visibility = View.VISIBLE
                binding.lavViewOg.playAnimation()
            }
            2 -> {
                binding.imgState.setImageResource(R.drawable.ic_main_disconnect)
                binding.txtTimerOg.setTextColor(getColor(R.color.vpn_success))
                OgTimerThread.startTiming()
                binding.lavViewOg.pauseAnimation()
                binding.lavViewOg.visibility = View.GONE
            }
        }
    }

    private fun showVpnGuide() {
        lifecycleScope.launch {
            delay(300)
            if (state.name != "Connected") {
                binding.homeGuideOg = true
                binding.lavViewGu.playAnimation()
            } else {
                binding.homeGuideOg = false
                binding.lavViewGu.pauseAnimation()
            }
        }
    }
    /**
     * 判断Vpn方案
     */
    private fun judgeVpnScheme() {
        if (!viewModel.isItABuyingUser()) {
            //非买量用户直接走A方案
            whetherToImplementPlanA = true
            return
        }
        val data = onlineConfig.online_ratio
        if (Utils.isNullOrEmpty(data)) {
            KLog.d(logTagOg, "判断Vpn方案---默认")
            vpnCScheme("50")
        } else {
            //C
            whetherToImplementPlanA = false
            vpnCScheme(data)
        }
    }

    /**
     * vpn B 方案
     */
    private fun vpnBScheme() {
        lifecycleScope.launch {
            delay(300)
            if (!state.canStop) {
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
            whetherToImplementPlanA = true
        } else {
            val random = (0..100).shuffled().last()
            when {
                random <= mProbabilityInt -> {
                    //B
                    KLog.d(logTagOg, "随机落在B方案")
                    vpnBScheme() //20，代表20%为B用户；80%为A用户
                }
                else -> {
                    //A
                    KLog.d(logTagOg, "随机落在A方案")
                    whetherToImplementPlanA = true
                }
            }
        }
    }
    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        changeState(state)
    }

    override fun onServiceConnected(service: IShadowsocksService) {
        changeState(
            try {
                BaseService.State.values()[service.state]
            } catch (_: RemoteException) {
                BaseService.State.Idle
            }
        )
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
                    KLog.d(logTagOg, "onResume------>1")
                    OgLoadHomeAd.setDisplayHomeNativeAdOg(this@MainActivity, binding)
                } else {
                    KLog.d(logTagOg, "onResume------>2")
                    AdBase.getHomeInstance().advertisementLoadingOg(this@MainActivity)
                    initHomeAd()
                }
            }
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
        LiveEventBus
            .get(Constant.PLUG_OG_ADVERTISEMENT_SHOW, Boolean::class.java)
            .removeObserver {}
        DataStore.publicStore.unregisterChangeListener(this)
        connection.disconnect(this)
        jobStartOg?.cancel()
        jobStartOg = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0x11 && whetherRefreshServer) {
            setFastInformation(viewModel.afterDisconnectionServerData)
            val serviceData = toJson(viewModel.afterDisconnectionServerData)
            MmkvUtils.set("currentServerData", serviceData)
            viewModel.currentServerData = viewModel.afterDisconnectionServerData
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (binding.viewGuideMask.isVisible) {
                binding.homeGuideOg = false
                binding.lavViewGu.pauseAnimation()
            } else {
                if (!(state.name != "Connected" && binding.vpnState == 1)) {
                    finish()
                }
            }
        }
        return true
    }
    /**
     * 心跳上报(链接)
     */
    private fun getHeartbeatReportedConnect(isConnected:Boolean) {
        jobHeart?.cancel()
        jobHeart = null
        jobHeart =lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                var data: String
                var ip: String
                if (isConnected) {
                    data = "as"
                    ip = mmkvOg.decodeString(Constant.IP_AFTER_VPN_LINK_OG,"")?:""
                } else {
                    data = "is"
                    ip = mmkvOg.decodeString(Constant.CURRENT_IP_OG,"")?:""
                }
                if (isConnected) {
                    OnlineOkHttpUtils.getHeartbeatReporting(data, ip)
                }
                delay(60000)
            }
        }
    }

    /**
     * 心跳上报(断开)
     */
    private fun getHeartbeatReportedDisConnect() {
        jobHeart?.cancel()
        jobHeart = null
        OnlineOkHttpUtils.getHeartbeatReporting("is", mmkvOg.decodeString(Constant.CURRENT_IP_OG,"")?:"")
    }
}