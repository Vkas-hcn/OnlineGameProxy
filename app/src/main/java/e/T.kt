package e

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import op.asd.bean.OgVpnBean
import op.asd.utils.OnlineGameUtils.getDataFromTheServer
import op.asd.utils.OnlineGameUtils.getFastIpOg
import com.xuexiang.xutil.net.JsonUtil

class T  : ViewModel() {
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