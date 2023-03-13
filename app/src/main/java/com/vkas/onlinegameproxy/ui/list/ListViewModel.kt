package com.vkas.onlinegameproxy.ui.list

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.reflect.TypeToken
import com.vkas.onlinegameproxy.app.App.Companion.mmkvOg
import com.vkas.onlinegameproxy.base.BaseViewModel
import com.vkas.onlinegameproxy.bean.OgVpnBean
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.utils.KLog
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.getFastIpOg
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.getLocalServerData
import com.xuexiang.xui.utils.Utils.isNullOrEmpty
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
        skServiceBeanList = if (isNullOrEmpty(mmkvOg.decodeString(Constant.PROFILE_OG_DATA))) {
            KLog.e("TAG","skServiceBeanList--1--->")
            getLocalServerData()
        } else {
            KLog.e("TAG","skServiceBeanList--2--->")

            JsonUtil.fromJson(
                mmkvOg.decodeString(Constant.PROFILE_OG_DATA),
                object : TypeToken<MutableList<OgVpnBean>?>() {}.type
            )
        }
        skServiceBeanList.add(0, getFastIpOg())
        KLog.e("LOG","skServiceBeanList---->${JsonUtil.toJson(skServiceBeanList)}")

        liveServerListData.postValue(skServiceBeanList)
    }
}