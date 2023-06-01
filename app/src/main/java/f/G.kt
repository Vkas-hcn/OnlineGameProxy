package f

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.gson.reflect.TypeToken
import com.lsxiao.apollo.core.annotations.Receive
import op.asd.R
import op.asd.ad.OgLoadResultAd
import b.C
import b.C.Companion.mmkvOg
import op.asd.base.AdBase
import op.asd.base.BaseActivityNew
import op.asd.bean.OgVpnBean
import op.asd.key.Constant
import op.asd.utils.OnlineGameUtils
import com.xuexiang.xutil.net.JsonUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class G : BaseActivityNew() {
    private var isConnectionOg: Boolean = false

    //当前服务器
    private lateinit var currentServerBeanOg: OgVpnBean
    private var jobResultOg: Job? = null


    override fun getLayoutId(): Int {
        return R.layout.activity_result_og
    }

    private var vpnState = false
    private val ogAdFrame: FrameLayout by bindView(R.id.og_ad_frame)
    private val imgOgAdFrame: ImageView by bindView(R.id.img_og_ad_frame)
    private val txtTimerOg: TextView by bindView(R.id.txt_timer_og)
    private val txtTimerOgDis: TextView by bindView(R.id.txt_timer_og_dis)
    private val tvConnected: TextView by bindView(R.id.tv_connected)
    private val imgConnectState: ImageView by bindView(R.id.img_connect_state)
    private val imgCountry: ImageView by bindView(R.id.img_country)
    private val resultTitle: View by bindView(R.id.result_title)

    private lateinit var titleBack: ImageView
    private lateinit var titleText: TextView

    fun resultAdOgFun(resultAdOg: Boolean) {
        if (resultAdOg) {
            ogAdFrame.visibility = View.VISIBLE
            imgOgAdFrame.visibility = View.GONE
        } else {
            ogAdFrame.visibility = View.GONE
            imgOgAdFrame.visibility = View.VISIBLE
        }
    }

    fun vpnStateFun(vpnState: Boolean) {
        if (vpnState) {
            txtTimerOg.visibility = View.VISIBLE
            txtTimerOgDis.visibility = View.GONE
        } else {
            txtTimerOg.visibility = View.GONE
            txtTimerOgDis.visibility = View.VISIBLE
        }
    }

    override fun initData() {
        super.initData()
        val bundle = intent.extras
        isConnectionOg = bundle?.getBoolean(Constant.CONNECTION_OG_STATUS) == true
        currentServerBeanOg = JsonUtil.fromJson(
            bundle?.getString(Constant.SERVER_OG_INFORMATION),
            object : TypeToken<OgVpnBean?>() {}.type
        )
        titleBack = resultTitle.findViewById(R.id.img_back)
        titleText = resultTitle.findViewById(R.id.tv_title)
        titleBack.setOnClickListener {
            finish()
        }
//        binding.resultTitle.imgBack.setOnClickListener {
//            finish()
//        }
        resultAdOgFun(isConnectionOg)
        vpnStateFun(isConnectionOg)

        if (isConnectionOg) {
            tvConnected.text = getString(R.string.connecteds)
            titleText.text = getString(R.string.vpn_connect)

        } else {
            tvConnected.text = getString(R.string.disconnecteds)
            titleText.text = getString(R.string.vpn_disconnect)

            txtTimerOgDis.text = mmkvOg.decodeString(Constant.LAST_TIME, "").toString()
        }
        imgCountry.setImageResource(
            OnlineGameUtils.getFlagThroughCountryEc(
                currentServerBeanOg.ongpro_country.toString()
            )
        )
        AdBase.getResultInstance().whetherToShowOg = false
        initResultAds()
    }

    private fun initResultAds() {
        resultAdOgFun(false)
        jobResultOg = lifecycleScope.launch {
            while (isActive) {
                OgLoadResultAd.setDisplayResultNativeAd(
                    this@G,
                    ogAdFrame,
                    imgOgAdFrame
                )
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
            if (C.nativeAdRefreshOg) {
                AdBase.getResultInstance().whetherToShowOg = false
                if (AdBase.getResultInstance().appAdDataOg != null) {
                    OgLoadResultAd.setDisplayResultNativeAd(
                        this@G,
                        ogAdFrame,
                        imgOgAdFrame
                    )
                } else {
                    AdBase.getResultInstance().advertisementLoadingOg(this@G)
                    initResultAds()
                }
            }
        }
    }

    /**
     * 显示计时器
     */
    @Receive(Constant.TIMER_OG_DATA)
     fun displayTimer(it:String) {
        if (it != "00:00:00") {
            if (isConnectionOg) {
                txtTimerOg.text = it
            }
        }
    }

}