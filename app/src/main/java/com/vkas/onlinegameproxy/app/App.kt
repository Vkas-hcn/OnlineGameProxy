package com.vkas.onlinegameproxy.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdActivity
import com.tencent.mmkv.MMKV
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.utils.ActivityUtils
import com.vkas.onlinegameproxy.utils.CalendarUtils
import com.vkas.onlinegameproxy.utils.KLogUtils
import com.vkas.onlinegameproxy.utils.MmkvUtils
import java.util.*

class App : Application(), LifecycleObserver {


    companion object {
         var ad_activity_og: Activity? = null
        var top_activity_og: Activity? = null

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
        AppFun.initAppNoMain(this)
        setActivityLifecycleOg(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        AppFun.initApp(this)

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
       AppFun.appOnStart()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStopState() {
        AppFun.appOnStop()
    }



    fun setActivityLifecycleOg(application: Application) {
        //注册监听每个activity的生命周期,便于堆栈式管理
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                ActivityUtils.addActivity(activity, activity::class.java)
                if (activity !is AdActivity) {
                    top_activity_og = activity
                } else {
                    ad_activity_og = activity
                }
            }

            override fun onActivityStarted(activity: Activity) {
                if (activity !is AdActivity) {
                    top_activity_og = activity
                } else {
                    ad_activity_og = activity
                }
                AppFun.flag++
                isBackDataOg = false
            }

            override fun onActivityResumed(activity: Activity) {
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
            }

            override fun onActivityStopped(activity: Activity) {
                AppFun.flag--
                if (AppFun.flag == 0) {
                    isBackDataOg = true
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

            }

            override fun onActivityDestroyed(activity: Activity) {
                ActivityUtils.removeActivity(activity)
                ad_activity_og = null
                top_activity_og = null
            }
        })
    }
}