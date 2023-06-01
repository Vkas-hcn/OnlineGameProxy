package com.vkas.onlinegameproxy.ui.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vkas.onlinegameproxy.bean.OgVpnBean
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.getDataFromTheServer
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.getFastIpOg
import com.xuexiang.xutil.net.JsonUtil

class ListViewModel  : ViewModel() {
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
        liveServerListData.postValue(skServiceBeanList)
    }
}