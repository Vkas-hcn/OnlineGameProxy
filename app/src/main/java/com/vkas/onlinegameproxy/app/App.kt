package com.vkas.onlinegameproxy.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.blankj.utilcode.util.ProcessUtils
import com.github.shadowsocks.Core
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.jeremyliao.liveeventbus.LiveEventBus
import com.tencent.mmkv.MMKV
import com.vkas.onlinegameproxy.BuildConfig
import com.vkas.onlinegameproxy.base.AppManagerOgMVVM
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.ui.main.MainActivity
import com.vkas.onlinegameproxy.ui.start.StartActivity
import com.vkas.onlinegameproxy.utils.ActivityUtils
import com.vkas.onlinegameproxy.utils.CalendarUtils
import com.vkas.onlinegameproxy.utils.KLog
import com.vkas.onlinegameproxy.utils.MmkvUtils
import com.vkas.onlinegameproxy.utils.OgTimerThread.sendTimerInformation
import com.xuexiang.xui.XUI
import com.xuexiang.xutil.XUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class App : Application(), LifecycleObserver {
    private var flag = 0
    private var job_og : Job? =null
    private var ad_activity_og: Activity? = null
    private var top_activity_og: Activity? = null
    companion object {
        // app当前是否在后台
        var isBackDataOg = false
        // VPN是否链接
        var isVpnGlobalLink = false
        // 是否进入后台（三秒后）
        var whetherBackgroundOg = false
        // 原生广告刷新
        var nativeAdRefreshOg = false
        val mmkvOg by lazy {
            //启用mmkv的多进程功能
            MMKV.mmkvWithID("OnlineGameProxy", MMKV.MULTI_PROCESS_MODE)
        }
        //当日日期
        var adDateOg = ""
        /**
         * 判断是否是当天打开
         */
        fun isAppOpenSameDayOg() {
            adDateOg = mmkvOg.decodeString(Constant.CURRENT_OG_DATE, "").toString()
            if (adDateOg == "") {
                MmkvUtils.set(Constant.CURRENT_OG_DATE, CalendarUtils.formatDateNow())
            } else {
                if (CalendarUtils.dateAfterDate(adDateOg, CalendarUtils.formatDateNow())) {
                    MmkvUtils.set(Constant.CURRENT_OG_DATE, CalendarUtils.formatDateNow())
                    MmkvUtils.set(Constant.CLICKS_OG_COUNT, 0)
                    MmkvUtils.set(Constant.SHOW_OG_COUNT, 0)
                }
            }
        }
    }
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        setActivityLifecycleOg(this)
        MobileAds.initialize(this) {}
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        if (ProcessUtils.isMainProcess()) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
            Firebase.initialize(this)
            FirebaseApp.initializeApp(this)
            XUI.init(this) //初始化UI框架
            XUtil.init(this)
            LiveEventBus
                .config()
                .lifecycleObserverAlwaysActive(true)
            //是否开启打印日志
            KLog.init(BuildConfig.DEBUG)
        }
        Core.init(this, MainActivity::class)
        sendTimerInformation()
        isAppOpenSameDayOg()
        val data = mmkvOg.decodeString(Constant.UUID_VALUE_OG,null)
        if(data.isNullOrEmpty()){
            MmkvUtils.set(Constant.UUID_VALUE_OG, UUID.randomUUID().toString())
        }
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        nativeAdRefreshOg =true
        job_og?.cancel()
        job_og = null
        //从后台切过来，跳转启动页
        if (whetherBackgroundOg && !isBackDataOg) {
            jumpGuidePage()
        }
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStopState(){
        job_og = GlobalScope.launch {
            whetherBackgroundOg = false
            delay(3000L)
            whetherBackgroundOg = true
            ad_activity_og?.finish()
            ActivityUtils.getActivity(StartActivity::class.java)?.finish()
        }
    }
    /**
     * 跳转引导页
     */
    private fun jumpGuidePage(){
        whetherBackgroundOg = false
        val intent = Intent(top_activity_og, StartActivity::class.java)
        intent.putExtra(Constant.RETURN_OG_CURRENT_PAGE, true)
        top_activity_og?.startActivity(intent)
    }
    fun setActivityLifecycleOg(application: Application) {
        //注册监听每个activity的生命周期,便于堆栈式管理
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                AppManagerOgMVVM.get().addActivity(activity)
                if (activity !is AdActivity) {
                    top_activity_og = activity
                } else {
                    ad_activity_og = activity
                }
                KLog.v("Lifecycle", "onActivityCreated" + activity.javaClass.name)
            }

            override fun onActivityStarted(activity: Activity) {
                KLog.v("Lifecycle", "onActivityStarted" + activity.javaClass.name)
                if (activity !is AdActivity) {
                    top_activity_og = activity
                } else {
                    ad_activity_og = activity
                }
                flag++
                isBackDataOg = false
            }

            override fun onActivityResumed(activity: Activity) {
                KLog.v("Lifecycle", "onActivityResumed=" + activity.javaClass.name)
                if (activity !is AdActivity) {
                    top_activity_og = activity
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (activity is AdActivity) {
                    ad_activity_og = activity
                } else {
                    top_activity_og = activity
                }
                KLog.v("Lifecycle", "onActivityPaused=" + activity.javaClass.name)
            }

            override fun onActivityStopped(activity: Activity) {
                flag--
                if (flag == 0) {
                    isBackDataOg = true
                }
                KLog.v("Lifecycle", "onActivityStopped=" + activity.javaClass.name)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                KLog.v("Lifecycle", "onActivitySaveInstanceState=" + activity.javaClass.name)

            }

            override fun onActivityDestroyed(activity: Activity) {
                AppManagerOgMVVM.get().removeActivity(activity)
                KLog.v("Lifecycle", "onActivityDestroyed" + activity.javaClass.name)
                ad_activity_og = null
                top_activity_og = null
            }
        })
    }
}