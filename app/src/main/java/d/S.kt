package d

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import b.C
import op.asd.key.Constant
import op.asd.utils.OnlineOkHttpUtils
import kotlinx.coroutines.*

object S {
    var vpnState: Int = 0
    private var jobHeart: Job? = null
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
                    ip = C.mmkvOg.decodeString(Constant.IP_AFTER_VPN_LINK_OG, "") ?: ""
                } else {
                    data = "is"
                    ip = C.mmkvOg.decodeString(Constant.CURRENT_IP_OG, "") ?: ""
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
            C.mmkvOg.decodeString(Constant.CURRENT_IP_OG, "") ?: ""
        )
    }

}