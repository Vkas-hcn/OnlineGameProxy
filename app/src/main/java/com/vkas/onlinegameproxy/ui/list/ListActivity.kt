package com.vkas.onlinegameproxy.ui.list

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.onlinegameproxy.BR
import com.vkas.onlinegameproxy.R
import com.vkas.onlinegameproxy.ad.OgLoadBackAd
import com.vkas.onlinegameproxy.ad.OgLoadListAd
import com.vkas.onlinegameproxy.app.App
import com.vkas.onlinegameproxy.base.AdBase
import com.vkas.onlinegameproxy.bean.OgVpnBean
import com.vkas.onlinegameproxy.bean.OpRemoteBean
import com.vkas.onlinegameproxy.databinding.ActivityServiceListOgBinding
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.key.Constant.logTagOg
import com.vkas.onlinegameproxy.utils.KLog
import com.vkas.onlinegameproxy.utils.OnlineGameUtils
import com.xuexiang.xutil.net.JsonUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class ListActivity : BaseActivity<ActivityServiceListOgBinding, ListViewModel>() {
    private lateinit var selectAdapter: ListAdapter
    private var ecServiceBeanList: MutableList<OgVpnBean> = ArrayList()
    private lateinit var adBean: OgVpnBean
    private var jobBackOg: Job? = null

    //选中服务器
    private lateinit var checkSkServiceBean: OgVpnBean
    private lateinit var checkSkServiceBeanClick: OgVpnBean
    val onlineConfig: OpRemoteBean = OnlineGameUtils.getLocalVpnBootData()

    // 是否连接
    private var whetherToConnect = false

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_service_list_og
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
        val bundle = intent.extras
        checkSkServiceBean = OgVpnBean()
        whetherToConnect = bundle?.getBoolean(Constant.WHETHER_OG_CONNECTED) == true
        checkSkServiceBean = JsonUtil.fromJson(
            bundle?.getString(Constant.CURRENT_OG_SERVICE),
            object : TypeToken<OgVpnBean?>() {}.type
        )
        checkSkServiceBeanClick = checkSkServiceBean
    }

    override fun initToolbar() {
        super.initToolbar()
        liveEventBusReceive()
        binding.selectTitleOg.tvTitle.text = getString(R.string.locations)
        binding.selectTitleOg.imgBack.setOnClickListener {
            returnToHomePage()
        }
    }

    override fun initData() {
        super.initData()
        initSelectRecyclerView()
        viewModel.getServerListData()
        AdBase.getBackInstance().whetherToShowOg = false
        AdBase.getListInstance().whetherToShowOg = false
        initListAds()
    }

    override fun initViewObservable() {
        super.initViewObservable()
        getServerListData()
    }

    private fun liveEventBusReceive() {
        //插屏关闭后跳转
        LiveEventBus
            .get(Constant.PLUG_OG_BACK_AD_SHOW, Boolean::class.java)
            .observeForever {
                finish()
            }
    }

    private fun initListAds() {
        binding.listAdOg = false
        jobBackOg = lifecycleScope.launch {
            while (isActive) {
                OgLoadListAd.setDisplayListNativeAdOg(this@ListActivity, binding)
                if (AdBase.getListInstance().whetherToShowOg) {
                    jobBackOg?.cancel()
                    jobBackOg = null
                }
                delay(1000L)
            }
        }
    }


    private fun getServerListData() {
        viewModel.liveServerListData.observe(this, {
            echoServer(it)
        })
    }

    private fun initSelectRecyclerView() {
        selectAdapter = ListAdapter(ecServiceBeanList)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.recyclerSelect.layoutManager = layoutManager
        binding.recyclerSelect.adapter = selectAdapter
        selectAdapter.setOnItemClickListener { _, _, pos ->
            run {
                selectServer(pos)
            }
        }
    }


    /**
     * 选中服务器
     */
    private fun selectServer(position: Int) {
        if (ecServiceBeanList[position].ongpro_ip == checkSkServiceBeanClick.ongpro_ip && ecServiceBeanList[position].og_best == checkSkServiceBeanClick.og_best) {
            if (!whetherToConnect) {
                finish()
                LiveEventBus.get<OgVpnBean>(Constant.NOT_CONNECTED_OG_RETURN)
                    .post(checkSkServiceBean)
            }
            return
        }
        ecServiceBeanList.forEachIndexed { index, _ ->
            ecServiceBeanList[index].og_check = position == index
            if (ecServiceBeanList[index].og_check == true) {
                checkSkServiceBean = ecServiceBeanList[index]
            }
        }
        selectAdapter.notifyDataSetChanged()
        showDisconnectDialog()
    }

    /**
     * 回显服务器
     */
    private fun echoServer(it: MutableList<OgVpnBean>) {
        ecServiceBeanList = it
        ecServiceBeanList.forEachIndexed { index, _ ->
            if (checkSkServiceBeanClick.og_best == true) {
                ecServiceBeanList[0].og_check = true
            } else {
                ecServiceBeanList[index].og_check =
                    ecServiceBeanList[index].ongpro_ip == checkSkServiceBeanClick.ongpro_ip
                ecServiceBeanList[0].og_check = false
            }
        }
        KLog.e("TAG", "ecServiceBeanList=${JsonUtil.toJson(ecServiceBeanList)}")
        selectAdapter.setList(ecServiceBeanList)
    }

    /**
     * 返回主页
     */
    private fun returnToHomePage() {
        App.isAppOpenSameDayOg()
        if (OnlineGameUtils.isThresholdReached()) {
            KLog.d(logTagOg, "广告达到上线")
            finish()
            return
        }
        if (OnlineGameUtils.whetherToBlockScreenAds(onlineConfig.online_ref)) {
            if (OgLoadBackAd.displayBackAdvertisementOg(this) != 2) {
                finish()
            }
        } else {
            finish()
        }
    }

    /**
     * 是否断开连接
     */
    private fun showDisconnectDialog() {
        if (!whetherToConnect) {
            finish()
            LiveEventBus.get<OgVpnBean>(Constant.NOT_CONNECTED_OG_RETURN)
                .post(checkSkServiceBean)
            return
        }
        val dialog: AlertDialog? = AlertDialog.Builder(this)
            .setTitle("Are you sure to disconnect current server")
            //设置对话框的按钮
            .setNegativeButton("CANCEC") { dialog, _ ->
                dialog.dismiss()
                ecServiceBeanList.forEachIndexed { index, _ ->
                    ecServiceBeanList[index].og_check =
                        (ecServiceBeanList[index].ongpro_ip == checkSkServiceBeanClick.ongpro_ip && ecServiceBeanList[index].og_best == checkSkServiceBeanClick.og_best)
                }
                selectAdapter.notifyDataSetChanged()
            }
            .setPositiveButton("DISCONNECT") { dialog, _ ->
                dialog.dismiss()
                finish()
                LiveEventBus.get<OgVpnBean>(Constant.CONNECTED_OG_RETURN)
                    .post(checkSkServiceBean)
            }.create()
        val params = dialog!!.window!!.attributes
        params.width = 200
        params.height = 200
        dialog.window!!.attributes = params
        dialog.setCancelable(false)
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(300)
            if (lifecycle.currentState != Lifecycle.State.RESUMED) {
                return@launch
            }
            if (App.nativeAdRefreshOg) {
                AdBase.getListInstance().whetherToShowOg = false
                if (AdBase.getListInstance().appAdDataOg != null) {
                    OgLoadListAd.setDisplayListNativeAdOg(this@ListActivity, binding)
                } else {
                    AdBase.getResultInstance().advertisementLoadingOg(this@ListActivity)
                    initListAds()
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            returnToHomePage()
        }
        return true
    }
}