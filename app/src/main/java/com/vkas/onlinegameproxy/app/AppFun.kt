package com.vkas.onlinegameproxy.app

import android.app.Application
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import com.blankj.utilcode.util.ProcessUtils
import com.github.shadowsocks.Core
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.lsxiao.apollo.core.Apollo
import com.tencent.mmkv.MMKV
import com.vkas.onlinegameproxy.BuildConfig
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.ui.main.MainActivity
import com.vkas.onlinegameproxy.ui.start.StartActivity
import com.vkas.onlinegameproxy.utils.ActivityUtils
import com.vkas.onlinegameproxy.utils.MmkvUtils
import com.vkas.onlinegameproxy.utils.OgTimerThread
import com.xuexiang.xui.XUI
import com.xuexiang.xutil.XUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

object AppFun {
    var flag = 0
    var job_og: Job? = null
    fun initApp(application: Application) {
        if (ProcessUtils.isMainProcess()) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
            Firebase.initialize(application)
            FirebaseApp.initializeApp(application)
            XUI.init(application) //初始化UI框架
            XUtil.init(application)
        }
    }

    fun initAppNoMain(application: Application) {
        Apollo.init(AndroidSchedulers.mainThread(), application)
        MMKV.initialize(application)
        MobileAds.initialize(application) {}
        Core.init(application, MainActivity::class)
        OgTimerThread.sendTimerInformation()
        App.isAppOpenSameDayOg()
        val data = App.mmkvOg.decodeString(Constant.UUID_VALUE_OG, null)
        if (data.isNullOrEmpty()) {
            MmkvUtils.set(Constant.UUID_VALUE_OG, UUID.randomUUID().toString())
        }
    }

    fun appOnStart() {
        App.nativeAdRefreshOg = true
        job_og?.cancel()
        job_og = null
        //从后台切过来，跳转启动页
        if (App.whetherBackgroundOg && !App.isBackDataOg) {
            jumpGuidePage()
        }
    }

    fun appOnStop() {
        job_og = GlobalScope.launch {
            App.whetherBackgroundOg = false
            delay(3000L)
            App.whetherBackgroundOg = true
            App.ad_activity_og?.finish()
            ActivityUtils.getActivity(StartActivity::class.java)?.finish()
        }
    }

    /**
     * 跳转引导页
     */
    private fun jumpGuidePage() {
        App.whetherBackgroundOg = false
        val intent = Intent(App.top_activity_og, StartActivity::class.java)
        intent.putExtra(Constant.RETURN_OG_CURRENT_PAGE, true)
        App.top_activity_og?.startActivity(intent)
    }
}