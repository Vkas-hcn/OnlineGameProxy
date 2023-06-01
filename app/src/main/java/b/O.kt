package b

import android.app.Application
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import com.blankj.utilcode.util.ProcessUtils
import h.V
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.lsxiao.apollo.core.Apollo
import com.tencent.mmkv.MMKV
import op.asd.BuildConfig
import op.asd.key.Constant
import d.E
import c.D
import op.asd.utils.ActivityUtils
import op.asd.utils.MmkvUtils
import op.asd.utils.OgTimerThread
import com.xuexiang.xui.XUI
import com.xuexiang.xutil.XUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

object O {
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
        V.init(application, E::class)
        OgTimerThread.sendTimerInformation()
        C.isAppOpenSameDayOg()
        val data = C.mmkvOg.decodeString(Constant.UUID_VALUE_OG, null)
        if (data.isNullOrEmpty()) {
            MmkvUtils.set(Constant.UUID_VALUE_OG, UUID.randomUUID().toString())
        }
    }

    fun appOnStart() {
        C.nativeAdRefreshOg = true
        job_og?.cancel()
        job_og = null
        //从后台切过来，跳转启动页
        if (C.whetherBackgroundOg && !C.isBackDataOg) {
            jumpGuidePage()
        }
    }

    fun appOnStop() {
        job_og = GlobalScope.launch {
            C.whetherBackgroundOg = false
            delay(3000L)
            C.whetherBackgroundOg = true
            C.ad_activity_og?.finish()
            ActivityUtils.getActivity(D::class.java)?.finish()
        }
    }

    /**
     * 跳转引导页
     */
    private fun jumpGuidePage() {
        C.whetherBackgroundOg = false
        val intent = Intent(C.top_activity_og, D::class.java)
        intent.putExtra(Constant.RETURN_OG_CURRENT_PAGE, true)
        C.top_activity_og?.startActivity(intent)
    }
}