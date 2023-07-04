package com.vkas.onlinegameproxy.ui.main

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vkas.onlinegameproxy.app.App
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.utils.OnlineOkHttpUtils
import kotlinx.coroutines.*

object MainFun {
    var vpnState: Int = 0
    private var jobHeart: Job? = null
    // 点击时候的状态
    var statusAtTheTimeOfClick:String=""
    /**
     * 心跳上报(链接)
     */
     fun getHeartbeatReportedConnect(isConnected: Boolean,activity: AppCompatActivity) {
        jobHeart?.cancel()
        jobHeart = null
        jobHeart = activity.lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                var data: String
                var ip: String
                if (isConnected) {
                    data = "as"
                    ip = App.mmkvOg.decodeString(Constant.IP_AFTER_VPN_LINK_OG, "") ?: ""
                } else {
                    data = "is"
                    ip = App.mmkvOg.decodeString(Constant.CURRENT_IP_OG, "") ?: ""
                }
                if (isConnected) {
                    OnlineOkHttpUtils.getHeartbeatReporting(data, ip)
                }
                delay(60000)
            }
        }
    }

    /**
     * 心跳上报(断开)
     */
     fun getHeartbeatReportedDisConnect() {
        jobHeart?.cancel()
        jobHeart = null
        OnlineOkHttpUtils.getHeartbeatReporting(
            "is",
            App.mmkvOg.decodeString(Constant.CURRENT_IP_OG, "") ?: ""
        )
    }

}