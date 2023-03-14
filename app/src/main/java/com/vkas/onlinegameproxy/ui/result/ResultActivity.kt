package com.vkas.onlinegameproxy.ui.result

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.onlinegameproxy.BR
import com.vkas.onlinegameproxy.R
import com.vkas.onlinegameproxy.ad.OgLoadResultAd
import com.vkas.onlinegameproxy.app.App
import com.vkas.onlinegameproxy.app.App.Companion.mmkvOg
import com.vkas.onlinegameproxy.base.AdBase
import com.vkas.onlinegameproxy.base.BaseActivity
import com.vkas.onlinegameproxy.base.BaseViewModel
import com.vkas.onlinegameproxy.bean.OgVpnBean
import com.vkas.onlinegameproxy.databinding.ActivityResultOgBinding
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.utils.KLog
import com.vkas.onlinegameproxy.utils.OnlineGameUtils
import com.xuexiang.xutil.net.JsonUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ResultActivity : BaseActivity<ActivityResultOgBinding, BaseViewModel>() {
    private var isConnectionOg: Boolean = false

    //当前服务器
    private lateinit var currentServerBeanOg: OgVpnBean
    private var jobResultOg: Job? = null
    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_result_og
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
        val bundle = intent.extras
        isConnectionOg = bundle?.getBoolean(Constant.CONNECTION_OG_STATUS) == true
        currentServerBeanOg = JsonUtil.fromJson(
            bundle?.getString(Constant.SERVER_OG_INFORMATION),
            object : TypeToken<OgVpnBean?>() {}.type
        )
    }

    override fun initToolbar() {
        super.initToolbar()
        binding.resultTitle.imgBack.setOnClickListener {
            finish()
        }
    }

    override fun initData() {
        super.initData()
        binding.vpnState = isConnectionOg
        if (isConnectionOg) {
            binding.vpnState = true
            binding.tvConnected.text = getString(R.string.connecteds)
        } else {
            binding.tvConnected.text = getString(R.string.disconnecteds)
            binding.txtTimerOgDis.text = mmkvOg.decodeString(Constant.LAST_TIME, "").toString()
        }
        binding.imgCountry.setImageResource(
            OnlineGameUtils.getFlagThroughCountryEc(
                currentServerBeanOg.og_country.toString()
            )
        )
        AdBase.getResultInstance().whetherToShowOg = false
        initResultAds()
        displayTimer()
    }

    private fun initResultAds() {
        binding.resultAdOg = false
        jobResultOg = lifecycleScope.launch {
            while (isActive) {
                OgLoadResultAd.setDisplayResultNativeAd(this@ResultActivity, binding)
                if (AdBase.getResultInstance().whetherToShowOg) {
                    jobResultOg?.cancel()
                    jobResultOg = null
                }
                delay(1000L)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(300)
            if (lifecycle.currentState != Lifecycle.State.RESUMED) {
                return@launch
            }
            if (App.nativeAdRefreshOg) {
                AdBase.getResultInstance().whetherToShowOg = false
                if (AdBase.getResultInstance().appAdDataOg != null) {
                    OgLoadResultAd.setDisplayResultNativeAd(this@ResultActivity, binding)
                } else {
                    AdBase.getResultInstance().advertisementLoadingOg(this@ResultActivity)
                    initResultAds()
                }
            }
        }
    }

    /**
     * 显示计时器
     */
    private fun displayTimer() {
        LiveEventBus
            .get(Constant.TIMER_OG_DATA, String::class.java)
            .observeForever {
                KLog.e(
                    "TAG",
                    "isConnectionOg=$isConnectionOg---->${
                        mmkvOg.decodeString(
                            Constant.LAST_TIME,
                            ""
                        )
                    }"
                )
                if (it != "00:00:00") {
                    if (isConnectionOg) {
                        binding.txtTimerOg.text = it
                    }
                }
            }
    }

}