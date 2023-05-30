package com.vkas.onlinegameproxy.ui.web

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Color
import android.net.http.SslError
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.vkas.onlinegameproxy.BR
import com.vkas.onlinegameproxy.R
import com.vkas.onlinegameproxy.base.BaseActivityNew
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.widget.HorizontalProgressViewOg

class WebActivity : BaseActivityNew() {
    private val webTitleOgF: FrameLayout by bindView(R.id.web_title_og)
    private lateinit var webTitleOg: ImageView

    private val ppWebOg: WebView by bindView(R.id.pp_web_og)

    override fun getLayoutId(): Int {
        return R.layout.activity_web_og
    }

    override fun initData() {
        super.initData()
        webTitleOg =webTitleOgF.findViewById(R.id.img_back)
        webTitleOg.setOnClickListener {
            finish()
        }
        ppWebOg.loadUrl(Constant.PRIVACY_OG_AGREEMENT)
        ppWebOg.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            }

            override fun onPageFinished(view: WebView, url: String) {
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.proceed()
            }
        }

        ppWebOg.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler, error: SslError
            ) {
                val dialog: AlertDialog? = AlertDialog.Builder(this@WebActivity)
                    .setTitle("SSL authentication failed. Do you want to continue accessing?")
                    //设置对话框的按钮
                    .setNegativeButton("cancel") { dialog, _ ->
                        dialog.dismiss()
                        handler.cancel()
                    }
                    .setPositiveButton("continue") { dialog, _ ->
                        dialog.dismiss()
                        handler.cancel()
                    }.create()

                val params = dialog!!.window!!.attributes
                params.width = 200
                params.height = 200
                dialog.window!!.attributes = params
                dialog.show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (Constant.PRIVACY_OG_AGREEMENT == url) {
                    view.loadUrl(url)
                } else {
                    // 系统处理
                    return super.shouldOverrideUrlLoading(view, url)
                }
                return true
            }
        }


    }


    //点击返回上一页面而不是退出浏览器
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && ppWebOg.canGoBack()) {
            ppWebOg.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        ppWebOg.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
        ppWebOg.clearHistory()
        (ppWebOg.parent as ViewGroup).removeView(ppWebOg)
        ppWebOg.destroy()
        super.onDestroy()
    }
}