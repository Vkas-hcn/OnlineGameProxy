package com.vkas.onlinegameproxy.ui.main

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.google.gson.reflect.TypeToken
import com.vkas.onlinegameproxy.R
import com.vkas.onlinegameproxy.app.App.Companion.mmkvOg
import com.vkas.onlinegameproxy.base.AdBase
import com.vkas.onlinegameproxy.base.BaseViewModel
import com.vkas.onlinegameproxy.bean.OgIp2Bean
import com.vkas.onlinegameproxy.bean.OgIpBean
import com.vkas.onlinegameproxy.bean.OgVpnBean
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.utils.KLog
import com.vkas.onlinegameproxy.utils.MmkvUtils
import com.vkas.onlinegameproxy.utils.OnlineGameUtils
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.net.JsonUtil
import java.util.*

class MainViewModel (application: Application) : BaseViewModel(application) {
    //初始化服务器数据
    val liveInitializeServerData: MutableLiveData<OgVpnBean> by lazy {
        MutableLiveData<OgVpnBean>()
    }

    //更新服务器数据(未连接)
    val liveNoUpdateServerData: MutableLiveData<OgVpnBean> by lazy {
        MutableLiveData<OgVpnBean>()
    }

    //更新服务器数据(已连接)
    val liveUpdateServerData: MutableLiveData<OgVpnBean> by lazy {
        MutableLiveData<OgVpnBean>()
    }

    //当前服务器
    var currentServerData: OgVpnBean = OgVpnBean()

    //断开后选中服务器
    var afterDisconnectionServerData: OgVpnBean = OgVpnBean()

    //跳转结果页
    val liveJumpResultsPage: MutableLiveData<Bundle> by lazy {
        MutableLiveData<Bundle>()
    }

    fun initializeServerData() {
        val bestData = OnlineGameUtils.getFastIpOg()
        ProfileManager.getProfile(DataStore.profileId).let {
            if (it != null) {
                ProfileManager.updateProfile(setSkServerData(it, bestData))
            } else {
                val profile = Profile()
                ProfileManager.createProfile(setSkServerData(profile, bestData))
            }
        }
        DataStore.profileId = 1L
        currentServerData = bestData
        val serviceData = JsonUtil.toJson(currentServerData)
        MmkvUtils.set("currentServerData", serviceData)
        liveInitializeServerData.postValue(bestData)
    }

    fun updateSkServer(skServiceBean: OgVpnBean, isConnect: Boolean) {
        ProfileManager.getProfile(DataStore.profileId).let {
            if (it != null) {
                setSkServerData(it, skServiceBean)
                ProfileManager.updateProfile(it)
            } else {
                ProfileManager.createProfile(Profile())
            }
        }
        DataStore.profileId = 1L
        if (isConnect) {
            afterDisconnectionServerData = skServiceBean
            liveUpdateServerData.postValue(skServiceBean)
        } else {
            currentServerData = skServiceBean
            val serviceData = JsonUtil.toJson(currentServerData)
            MmkvUtils.set("currentServerData", serviceData)
            liveNoUpdateServerData.postValue(skServiceBean)
        }
    }

    /**
     * 设置服务器数据
     */
    private fun setSkServerData(profile: Profile, bestData: OgVpnBean): Profile {
        profile.name = bestData.ongpro_country + "-" + bestData.ongpro_city
        profile.host = bestData.ongpro_ip.toString()
        profile.password = bestData.ongpro_password!!
        profile.method = bestData.ongpro_cc!!
        profile.remotePort = bestData.ongpro_port!!
        return profile
    }

    /**
     * 跳转连接结果页
     */
    fun jumpConnectionResultsPage(isConnection: Boolean) {
        val bundle = Bundle()
        val serviceData = mmkvOg.decodeString("currentServerData", "").toString()
        bundle.putBoolean(Constant.CONNECTION_OG_STATUS, isConnection)
        bundle.putString(Constant.SERVER_OG_INFORMATION, serviceData)
        liveJumpResultsPage.postValue(bundle)
    }

    /**
     * 解析是否是非法ip；中国大陆ip、伊朗ip
     */
    fun whetherParsingIsIllegalIp(): Boolean {
        val data = mmkvOg.decodeString(Constant.IP_INFORMATION)
        return if (Utils.isNullOrEmpty(data)) {
            whetherParsingIsIllegalIp2()
        } else {
            val ptIpBean: OgIpBean = JsonUtil.fromJson(
                data,
                object : TypeToken<OgIpBean?>() {}.type
            )
            KLog.e("code", "ptIpBean.country_code==${ptIpBean.country_code}")
            return ptIpBean.country_code == "IR" || ptIpBean.country_code == "CN" ||
                    ptIpBean.country_code == "HK" || ptIpBean.country_code == "MO"
        }
    }

    private fun whetherParsingIsIllegalIp2(): Boolean {
        val data = mmkvOg.decodeString(Constant.IP_INFORMATION2)
        val locale = Locale.getDefault()
        val language = locale.language
        KLog.e("state", "language2=${language}")

        KLog.e("state", "data2=${data}===isNullOrEmpty=${Utils.isNullOrEmpty(data)}")
        return if (Utils.isNullOrEmpty(data)) {
            language == "zh" || language == "fa"
        } else {
            val ptIpBean: OgIp2Bean = JsonUtil.fromJson(
                data,
                object : TypeToken<OgIp2Bean?>() {}.type
            )
            KLog.e("state", "OgIp2Bean.cc==${ptIpBean.cc}")
            return ptIpBean.cc == "IR" || ptIpBean.cc == "CN" ||
                    ptIpBean.cc == "HK" || ptIpBean.cc == "MO"
        }
    }

    /**
     * 是否显示不能使用弹框
     */
    fun whetherTheBulletBoxCannotBeUsed(context: AppCompatActivity) {
        val dialogVpn: AlertDialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.vpn))
            .setMessage(context.getString(R.string.cant_user_vpn))
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                XUtil.exitApp()
            }.create()
        dialogVpn.setCancelable(false)
        dialogVpn.show()
        dialogVpn.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        dialogVpn.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
    }

    fun openInBrowser(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }
    /**
     *  清空所有广告重新加载
     */
    fun clearAllAdsReload(activity: Activity) {
        // 开屏
        AdBase.getOpenInstance().appAdDataOg = null
        AdBase.getOpenInstance().adIndexOg = 0
        AdBase.getOpenInstance().advertisementLoadingOg(activity)
        // 首页原生
        AdBase.getHomeInstance().appAdDataOg = null
        AdBase.getHomeInstance().adIndexOg = 0
        AdBase.getHomeInstance().advertisementLoadingOg(activity)
        // 结果页原生
        AdBase.getResultInstance().appAdDataOg = null
        AdBase.getResultInstance().adIndexOg = 0
        AdBase.getResultInstance().advertisementLoadingOg(activity)
        // 连接插屏
        AdBase.getConnectInstance().appAdDataOg = null
        AdBase.getConnectInstance().adIndexOg = 0
        AdBase.getConnectInstance().advertisementLoadingOg(activity)
        // 服务器页插屏
        AdBase.getBackInstance().appAdDataOg = null
        AdBase.getBackInstance().adIndexOg = 0
        AdBase.getBackInstance().advertisementLoadingOg(activity)
    }

    /**
     * 检测广告位加载
     */
    fun detectingAdSpaceLoading(activity: Activity) {
        // 首页原生
        AdBase.getHomeInstance().advertisementLoadingOg(activity)
        // 结果页原生
        AdBase.getResultInstance().advertisementLoadingOg(activity)
        // 连接插屏
        AdBase.getConnectInstance().advertisementLoadingOg(activity)
        // 服务器页插屏
        AdBase.getBackInstance().advertisementLoadingOg(activity)
    }
    /**
     * 是否是买量用户
     */
    fun isItABuyingUser(): Boolean {
        return OnlineGameUtils.isValuableUser()
    }
}