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
import com.vkas.onlinegameproxy.app.App
import com.vkas.onlinegameproxy.app.App.Companion.mmkvOg
import com.vkas.onlinegameproxy.base.BaseActivity
import com.vkas.onlinegameproxy.bean.OgVpnBean
import com.vkas.onlinegameproxy.databinding.ActivityMainBinding
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.key.Constant.logTagOg
import com.vkas.onlinegameproxy.ui.list.ListActivity
import com.vkas.onlinegameproxy.ui.result.ResultActivity
import com.vkas.onlinegameproxy.ui.web.WebActivity
import com.vkas.onlinegameproxy.utils.KLog
import com.vkas.onlinegameproxy.utils.MmkvUtils
import com.vkas.onlinegameproxy.utils.OgTimerThread
import com.vkas.onlinegameproxy.utils.OnlineGameUtils
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
//        LiveEventBus
//            .get(Constant.PLUG_OG_ADVERTISEMENT_SHOW, Boolean::class.java)
//            .observeForever {
//                KLog.e("state", "插屏关闭接收=${it}")
//                //重复点击
//                jobRepeatClick = lifecycleScope.launch {
//                    if (!repeatClick) {
//                        KLog.e("state", "插屏关闭后跳转=${it}")
//                        AdBase.getConnectInstance().advertisementLoadingOg(this@MainActivity)
//                        connectOrDisconnectOg(it)
//                        repeatClick = true
//                    }
//                    delay(1000)
//                    repeatClick = false
//                }
//            }
    }

    override fun initData() {
        super.initData()
        if (viewModel.whetherParsingIsIllegalIp()) {
            viewModel.whetherTheBulletBoxCannotBeUsed(this@MainActivity)
            return
        }

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

//        AdBase.getHomeInstance().whetherToShowOg = false

        // 初始化主页广告
        initHomeAd()

        // 显示VPN指南
        showVpnGuide()
    }

    private fun initHomeAd() {
//        jobNativeAdsOg = lifecycleScope.launch {
//            while (isActive) {
//                OgLoadHomeAd.setDisplayHomeNativeAdOg(this@MainActivity, binding)
//                if (AdBase.getHomeInstance().whetherToShowOg) {
//                    jobNativeAdsOg?.cancel()
//                    jobNativeAdsOg = null
//                }
//                delay(1000L)
//            }
//        }
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
            if (binding.vpnState != 1 && !binding.viewGuideMask.isVisible) {
                connect.launch(null)
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
            if (binding.vpnState != 1 && !binding.viewGuideMask.isVisible) {
                jumpToServerList()
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
//            AdBase.getBackInstance().advertisementLoadingOg(this@MainActivity)
            val serviceData = mmkvOg.decodeString("currentServerData", "").toString()
            bundle.putString(Constant.CURRENT_OG_SERVICE, serviceData)
            startActivity(ListActivity::class.java, bundle)
        }
    }

    /**
     * 设置fast信息
     */
    private fun setFastInformation(elVpnBean: OgVpnBean) {
        if (elVpnBean.og_best == true) {
            binding.txtCountry.text = Constant.FASTER_OG_SERVER
            binding.imgCountry.setImageResource(getFlagThroughCountryEc(Constant.FASTER_OG_SERVER))

        } else {
            binding.txtCountry.text = elVpnBean.og_country.toString()
            binding.imgCountry.setImageResource(getFlagThroughCountryEc(elVpnBean.og_country.toString()))

        }
    }

    private val connect = registerForActivityResult(StartService()) {
        binding.homeGuideOg = false
//        binding.viewGuideMask.visibility = View.GONE
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

//            AdBase.getConnectInstance().advertisementLoadingOg(this@MainActivity)
//            AdBase.getResultInstance().advertisementLoadingOg(this@MainActivity)

            try {
                withTimeout(10000L) {
                    delay(2000L)
//                    if (isThresholdReached() || Utils.isNullOrEmpty(OgLoadConnectAd.idOg)) {
//                        KLog.d(logTagOg, "广告达到上线,或者无广告位")
                        connectOrDisconnectOg(false)
//                        jobStartOg?.cancel()
//                        jobStartOg = null
//                        return@withTimeout
//                    }
//                    KLog.e(logTagOg, "jobStartOg?.isActive=${jobStartOg?.isActive}")
//                    while (jobStartOg?.isActive == true) {
//                        val showState =
//                            OgLoadConnectAd
//                                .displayConnectAdvertisementOg(this@MainActivity)
//                        if (showState) {
//                            jobStartOg?.cancel()
//                            jobStartOg = null
//                        }
//                        delay(1000L)
//                    }
                }
            } catch (e: TimeoutCancellationException) {
//                KLog.d(logTagOg, "connect---插屏超时")
//                connectOrDisconnectOg(false)
            }
        }
    }


    /**
     * 连接或断开
     * 是否后台关闭（true：后台关闭；false：手动关闭）
     */
    private fun connectOrDisconnectOg(isBackgroundClosed: Boolean) {
        KLog.e("state", "连接或断开")
        if (viewModel.whetherParsingIsIllegalIp()) {
            viewModel.whetherTheBulletBoxCannotBeUsed(this@MainActivity)
            return
        }
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
            if (!isBackgroundClosed) {
                viewModel.jumpConnectionResultsPage(true)
            }
//            EasyConnectUtils.getBuriedPointOg("unlimF_sv")
            if ((lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))) {
                Core.startService()
            } else {
                binding.vpnState = 0
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
//        EasyConnectUtils.getBuriedPointOg("unlimF_vT")
    }

    /**
     * 断开服务器
     */
    private fun disconnectServerSuccessful() {
        KLog.e("TAG", "断开服务器")
        binding.vpnState = 0
        changeOfVpnStatus()
//        if (clickToConnect) {
//            EasyConnectUtils.getBuriedPointConnectionTimeOg(
//                "unlimF_cn",
//                mmkvOg.decodeInt(Constant.LAST_TIME_SECOND)
//            )
//        }
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
//                binding.viewGuideMask.visibility = View.VISIBLE
                binding.lavViewGu.playAnimation()
            } else {
                binding.homeGuideOg = false
//                binding.viewGuideMask.visibility = View.GONE
                binding.lavViewGu.pauseAnimation()
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

//            if (App.nativeAdRefreshOg) {
//                changeOfVpnStatus()
//                AdBase.getHomeInstance().whetherToShowOg = false
//                if (AdBase.getHomeInstance().appAdDataOg != null) {
//                    KLog.d(logTagOg, "onResume------>1")
//                    OgLoadHomeAd.setDisplayHomeNativeAdOg(this@MainActivity, binding)
//                } else {
//                    binding.vpnAdOg = false
//                    KLog.d(logTagOg, "onResume------>2")
//                    AdBase.getHomeInstance().advertisementLoadingOg(this@MainActivity)
//                    initHomeAd()
//                }
//            }
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
}