package com.vkas.onlinegameproxy.ui.list

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.reflect.TypeToken
import com.lsxiao.apollo.core.Apollo
import com.lsxiao.apollo.core.annotations.Receive
import com.vkas.onlinegameproxy.R
import com.vkas.onlinegameproxy.ad.OgLoadBackAd
import com.vkas.onlinegameproxy.ad.OgLoadListAd
import com.vkas.onlinegameproxy.app.App
import com.vkas.onlinegameproxy.base.AdBase
import com.vkas.onlinegameproxy.base.BaseActivityNew
import com.vkas.onlinegameproxy.bean.OgVpnBean
import com.vkas.onlinegameproxy.bean.OpRemoteBean
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.key.Constant.logTagOg
import com.vkas.onlinegameproxy.utils.KLogUtils
import com.vkas.onlinegameproxy.utils.OnlineGameUtils
import com.xuexiang.xutil.net.JsonUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ListActivity : BaseActivityNew() {
    private lateinit var selectAdapter: ListAdapter
    private var ecServiceBeanList: MutableList<OgVpnBean> = ArrayList()
    private var jobBackOg: Job? = null
    val model by viewModels<ListViewModel>()

    //选中服务器
    private lateinit var checkSkServiceBean: OgVpnBean
    private lateinit var checkSkServiceBeanClick: OgVpnBean

    // 是否连接
    private var whetherToConnect = false
    private val ogAdFrame: FrameLayout by bindView(R.id.og_ad_frame)
    private val imgOgAdFrame: ImageView by bindView(R.id.img_og_ad_frame)
    private val recyclerSelect: RecyclerView by bindView(R.id.recycler_select)
    private lateinit var selectTitleOg: View
    private lateinit var titleBack: ImageView
    private lateinit var titleText: TextView

    override fun getLayoutId(): Int {
        return R.layout.activity_service_list_og
    }

    override fun initData() {
        super.initData()
        val bundle = intent.extras
        checkSkServiceBean = OgVpnBean()
        whetherToConnect = bundle?.getBoolean(Constant.WHETHER_OG_CONNECTED) == true
        checkSkServiceBean = JsonUtil.fromJson(
            bundle?.getString(Constant.CURRENT_OG_SERVICE),
            object : TypeToken<OgVpnBean?>() {}.type
        )
        checkSkServiceBeanClick = checkSkServiceBean

        selectTitleOg = findViewById(R.id.select_title_og)
        titleBack = selectTitleOg.findViewById(R.id.img_back)
        titleText = selectTitleOg.findViewById(R.id.tv_title)
        titleBack.setOnClickListener {
            returnToHomePage()
        }
        titleText.text = getString(R.string.locations)

        initSelectRecyclerView()
        model.getServerListData()
        // 服务器页插屏
        AdBase.getBackInstance().advertisementLoadingOg(this)
        AdBase.getBackInstance().whetherToShowOg = false
        AdBase.getListInstance().whetherToShowOg = false
        initListAds()
        getServerListData()

    }

    @Receive(Constant.PLUG_OG_BACK_AD_SHOW)
    fun liveEventBusReceive(it: Boolean) {
        //插屏关闭后跳转
        finish()
    }

    fun listAdOgFun(listAdOg: Boolean) {
        if (listAdOg) {
            ogAdFrame.visibility = View.VISIBLE
            imgOgAdFrame.visibility = View.GONE
        } else {
            ogAdFrame.visibility = View.GONE
            imgOgAdFrame.visibility = View.VISIBLE
        }
    }

    private fun initListAds() {
        listAdOgFun(false)
        jobBackOg = lifecycleScope.launch {
            while (isActive) {
                OgLoadListAd.setDisplayListNativeAdOg(this@ListActivity, ogAdFrame, imgOgAdFrame)
                if (AdBase.getListInstance().whetherToShowOg) {
                    jobBackOg?.cancel()
                    jobBackOg = null
                }
                delay(1000L)
            }
        }
    }


    private fun getServerListData() {
        model.liveServerListData.observe(this, {
            echoServer(it)
        })
    }

    private fun initSelectRecyclerView() {
        selectAdapter = ListAdapter(ecServiceBeanList)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerSelect.layoutManager = layoutManager
        recyclerSelect.adapter = selectAdapter
        selectAdapter.setOnItemClickListener(object : ListAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                selectServer(position)
            }
        })
    }


    /**
     * 选中服务器
     */
    private fun selectServer(position: Int) {
        if (ecServiceBeanList[position].ongpro_ip == checkSkServiceBeanClick.ongpro_ip
            && ecServiceBeanList[position].og_best == checkSkServiceBeanClick.og_best) {
            if (!whetherToConnect) {
                finish()
                Apollo.emit(Constant.NOT_CONNECTED_OG_RETURN, checkSkServiceBean)
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
//        selectAdapter.setList(ecServiceBeanList)
        selectAdapter.addData(ecServiceBeanList)
    }

    /**
     * 返回主页
     */
    private fun returnToHomePage() {
        if (OgLoadBackAd.displayBackAdvertisementOg(this) != 2) {
            finish()
        }
    }

    /**
     * 是否断开连接
     */
    private fun showDisconnectDialog() {
        if (!whetherToConnect) {
            finish()
            Apollo.emit(Constant.NOT_CONNECTED_OG_RETURN, checkSkServiceBean)

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
                Apollo.emit(Constant.CONNECTED_OG_RETURN, checkSkServiceBean)

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
                    OgLoadListAd.setDisplayListNativeAdOg(
                        this@ListActivity,
                        ogAdFrame,
                        imgOgAdFrame
                    )
                } else {
                    AdBase.getListInstance().advertisementLoadingOg(this@ListActivity)
                    initListAds()
                }
            }
            App.whetherHotStart = false
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            returnToHomePage()
        }
        return true
    }
}