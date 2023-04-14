package com.vkas.onlinegameproxy.key

object Constant {
    //滑动
    const val SLIDING = "sliding"
    // 分享地址
    const val SHARE_OG_ADDRESS="https://play.google.com/store/apps/details?id="
    // privacy_agreement
    const val PRIVACY_OG_AGREEMENT="https://www.baidu.com/"
    // email
    const val MAILBOX_OG_ADDRESS="vkas@qq.com"
    const val RETURN_OG_CURRENT_PAGE ="returnOgCurrentPage"
    // 广告数据
    const val ADVERTISING_OG_DATA="advertisingOgData"
    // 广告包名
    const val ADVERTISING_OG_PACKAGE="com.google.android.gms.ads.AdActivity"
    // 当日日期
    const val CURRENT_OG_DATE="currentOgDate"
    // 点击次数
    const val CLICKS_OG_COUNT="clicksOgCount"
    // 展示次数
    const val SHOW_OG_COUNT="showOgCount"
    //日志tag
    const val logTagOg = "logTagOg"
    //开屏关闭跳转
    const val OPEN_CLOSE_JUMP = "openCloseJump"
    //计时器数据
    const val TIMER_OG_DATA = "timerOgData"
    //停止vpn连接
    const val STOP_VPN_CONNECTION = "stopVpnConnection"
    // 最后时间（00：00：00）
    const val LAST_TIME = "lastTime"
    // 最后时间（秒）
    const val LAST_TIME_SECOND = "lastTimeSecond"
    //服务器信息
    const val SERVER_OG_INFORMATION = "serverOgInformation"
    //连接状态
    const val CONNECTION_OG_STATUS = "connectionOgStatus"
    //绕流数据
    const val AROUND_OG_FLOW_DATA = "aroundOgFlowData"
    // 服务器数据
    const val PROFILE_OG_DATA ="profileOgData"
    // 最佳服务器
    const val PROFILE_OG_DATA_FAST ="profileOgDataFast"
    // 是否已连接
    const val WHETHER_OG_CONNECTED="whetherOgConnected"
    // 当前服务器
    const val CURRENT_OG_SERVICE="currentOgService"
    // back插屏广告展示
    const val PLUG_OG_BACK_AD_SHOW="plugOgBackAdShow"
    // connect插屏广告展示
    const val PLUG_OG_ADVERTISEMENT_SHOW="plugOgAdvertisementShow"
    // Faster server
    const val FASTER_OG_SERVER= "Faster server"
    //ip信息
    const val IP_INFORMATION= "ipInformation"
    const val IP_INFORMATION2= "ipInformation2"
    // 已连接返回
    const val CONNECTED_OG_RETURN="connectedOgReturn"
    // 未连接返回
    const val NOT_CONNECTED_OG_RETURN="notConnectedOgReturn"
    // 广告本地文件名
    const val AD_LOCAL_FILE_NAME_SKY = "ogAdData.json"
    // vpn本地文件名
    const val VPN_LOCAL_FILE_NAME_SKY = "ogVpnData.json"
    // Fast本地文件名
    const val FAST_LOCAL_FILE_NAME_SKY = "ogVpnFastData.json"

    // vpn配置本地文件名
    const val VPN_BOOT_LOCAL_FILE_NAME_UF = "online_config.json"
    //online_config
    const val ONLINE_CONFIG = "online_config"
    //installReferrer
    const val INSTALL_REFERRER = "installReferrer"
}