package com.vkas.onlinegameproxy.net

import com.blankj.utilcode.util.AppUtils
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class OkHttpClientWrapper {
    private val client: OkHttpClient = OkHttpClient()

    interface Callback {
        fun onSuccess(response: String)
        fun onFailure(error: String)
    }

    fun get(url: String, callback: Callback) {
        val request = Request.Builder()
            .addHeader("PMM", AppUtils.getAppPackageName())
            .addHeader("NHH", "ZZ")
            .url(url)
            .build()

       client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    callback.onSuccess(responseBody)
                } else {
                    callback.onFailure(responseBody.toString())
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(e.toString())
            }
        })
    }

    fun getMap(url: String, map: Map<String, Any>, callback: Callback) {
        val urlBuilder = url.toHttpUrl().newBuilder()

        map.forEach { entry ->
            urlBuilder.addEncodedQueryParameter(
                entry.key,
                URLEncoder.encode(entry.value.toString(), StandardCharsets.UTF_8.toString())
            )
        }
        val request = Request.Builder()
            .addHeader("PMM", AppUtils.getAppPackageName())
            .addHeader("NHH", "ZZ")
            .get()
            .tag(map)
            .url(urlBuilder.build())
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    callback.onSuccess(responseBody)
                } else {
                    callback.onFailure(responseBody.toString())
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure("Network error")
            }
        })
    }

    fun post(url: String, body: Any, callback: Callback) {
        val requestBody =
            RequestBody.create("application/json".toMediaTypeOrNull(), body.toString())
        val request = Request.Builder()
            .addHeader("PMM", AppUtils.getAppPackageName())
            .addHeader("NHH", "ZZ")
            .url(url)
            .post(requestBody)
            .tag(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    callback.onSuccess(responseBody)
                } else {
                    callback.onFailure(responseBody.toString())
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure("Network error")
            }
        })
    }
}