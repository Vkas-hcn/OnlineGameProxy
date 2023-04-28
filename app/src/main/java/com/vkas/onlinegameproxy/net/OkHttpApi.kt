package com.vkas.onlinegameproxy.net
import androidx.collection.SimpleArrayMap
import com.google.gson.Gson
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
class OkHttpApi : HttpApi {

    var maxRetry = 0//最大重试 次数

    //存储请求，用于取消
    private val callMap = SimpleArrayMap<Any, Call>()

    //okHttpClient
    private val mClient = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)//完整请求超时时长，从发起到接收返回数据，默认值0，不限定,
        .connectTimeout(10, TimeUnit.SECONDS)//与服务器建立连接的时长，默认10s
        .readTimeout(10, TimeUnit.SECONDS)//读取服务器返回数据的时长
        .writeTimeout(10, TimeUnit.SECONDS)//向服务器写入数据的时长，默认10s
        .sslSocketFactory(SSLSocketClient.getSSLSocketFactory(),SSLSocketClient.IGNORE_SSL_TRUST_MANAGER_X509)
        .hostnameVerifier(SSLSocketClient.getHostnameVerifier())
        .retryOnConnectionFailure(true)//重连
        .followRedirects(false)//重定向
        .cache(Cache(File("sdcard/cache", "okhttp"), 1024))
//        .cookieJar(CookieJar.NO_COOKIES)
        .cookieJar(LocalCookieJar())
        .addNetworkInterceptor(HeaderInterceptor())//公共header的拦截器
        .addNetworkInterceptor(KtHttpLogInterceptor {
            logLevel(KtHttpLogInterceptor.LogLevel.BODY)
        })//添加网络拦截器，可以对okHttp的网络请求做拦截处理，不同于应用拦截器，这里能感知所有网络状态，比如重定向。
        .addNetworkInterceptor(RetryInterceptor(maxRetry))
//        .hostnameVerifier(HostnameVerifier { p0, p1 -> true })
//        .sslSocketFactory(sslSocketFactory = null,trustManager = null)
        .build()


    override fun get(
        params: Map<String, Any>,
        urlStr: String,
        callback: IHttpCallback,
        isUrlencoding: Boolean
    ) {
        val urlBuilder = urlStr.toHttpUrl().newBuilder()
        if (isUrlencoding) {
            params.forEach { entry ->
                urlBuilder.addEncodedQueryParameter(
                    entry.key,
                    URLEncoder.encode(entry.value.toString(), StandardCharsets.UTF_8.toString())
                )
            }
        } else {
            params.forEach { entry ->
                urlBuilder.addEncodedQueryParameter(entry.key, entry.value.toString())
            }
        }

        val request = Request.Builder()
            .get()
            .tag(params)
            .url(urlBuilder.build())
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()
        val newCall = mClient.newCall(request)
        //存储请求，用于取消
        callMap.put(request.tag(), newCall)
        newCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailed(e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 200) {
                    callback.onSuccess(response.body?.string())
                } else {
                    callback.onFailed(response.message)
                }
            }

        })
    }

    override fun post(body: Any, urlStr: String, callback: IHttpCallback) {

        val requestBody =
            RequestBody.create("application/json".toMediaTypeOrNull(), body.toString())

        val request = Request.Builder()
            .post(requestBody)
            .url(urlStr)
            .tag(body)
            .build()

        val newCall = mClient.newCall(request)
        //存储请求，用于取消
        callMap.put(request.tag(), newCall)
        newCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailed(e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 200) {
                    callback.onSuccess(response.body?.string())
                } else {
                    callback.onFailed(response.message)
                }
            }

        })
    }

    override fun postService(requestBody: FormBody, urlStr: String, callback: IHttpCallback) {
        val request = Request.Builder()
            .post(requestBody)
            .url(urlStr)
            .build()
        val newCall = mClient.newCall(request)
        //存储请求，用于取消
        callMap.put(request.tag(), newCall)
        newCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailed(e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 200) {
                    callback.onSuccess(response.body?.string())
                } else {
                    callback.onFailed(response.message)
                }
            }

        })
    }


    /**
     * 取消网络请求，tag就是每次请求的id 标记，也就是请求的传参
     */
    override fun cancelRequest(tag: Any) {
        callMap.get(tag)?.cancel()
    }


    /**
     * 取消所有网络请求
     */
    override fun cancelAllRequest() {
        for (i in 0 until callMap.size()) {
            callMap.get(callMap.keyAt(i))?.cancel()
        }
    }
}