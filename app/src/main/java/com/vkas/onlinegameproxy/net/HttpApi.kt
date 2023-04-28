package com.vkas.onlinegameproxy.net
import okhttp3.FormBody
import okhttp3.Request
interface HttpApi {
    /**
     * 抽象的http的get请求封装,异步
     */
    fun get(params: Map<String, Any>, urlStr: String, callback: IHttpCallback,isUrlencoding:Boolean=false)

    /**
     * 抽象的http同步的 get请求
     */
    fun getSync(params: Map<String, Any>, urlStr: String): Any? {
        return Any()
    }

    /**
     * 抽象的http的post的请求 异步
     */
    fun post(body: Any, urlStr: String, callback: IHttpCallback)

    /**
     * 抽象的http的post的请求 异步
     */
    fun postService(requestBody: FormBody, urlStr: String, callback: IHttpCallback)
    /**
     * 抽象的Http的post 同步请求
     */
    fun postSync(body: Any, urlStr: String): Any? = Any()


    fun cancelRequest(tag: Any)

    fun cancelAllRequest()
}