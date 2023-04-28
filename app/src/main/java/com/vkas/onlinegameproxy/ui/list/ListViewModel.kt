package com.vkas.onlinegameproxy.ui.list

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.reflect.TypeToken
import com.vkas.onlinegameproxy.app.App.Companion.mmkvOg
import com.vkas.onlinegameproxy.base.BaseViewModel
import com.vkas.onlinegameproxy.bean.OgVpnBean
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.utils.KLog
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.getDataFromTheServer
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.getFastIpOg
import com.xuexiang.xutil.net.JsonUtil

class ListViewModel (application: Application) : BaseViewModel(application) {
    private lateinit var skServiceBean : OgVpnBean
    private lateinit var skServiceBeanList :MutableList<OgVpnBean>

    // 服务器列表数据
    val liveServerListData: MutableLiveData<MutableList<OgVpnBean>> by lazy {
        MutableLiveData<MutableList<OgVpnBean>>()
    }

    /**
     * 获取服务器列表
     */
    fun getServerListData(){
        skServiceBeanList = ArrayList()
        skServiceBean = OgVpnBean()
        skServiceBeanList = getDataFromTheServer()!!

        skServiceBeanList.add(0, getFastIpOg())
        KLog.e("LOG","skServiceBeanList---->${JsonUtil.toJson(skServiceBeanList)}")

        liveServerListData.postValue(skServiceBeanList)
    }
}