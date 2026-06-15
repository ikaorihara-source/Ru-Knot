package com.ikaorihara.ruknot.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.live.bilibili.com/"

    var userCookie: String = ""

    // 创建一个监控器 (Interceptor)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // 设置级别为 BODY，意味着我们会打印所有的请求头和返回内容
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 伪装拦截器：把自己伪装成浏览器
    private val headerInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        // 检查是否已经存在 User-Agent。如果没有，才使用 PC 端伪装
        if (originalRequest.header("User-Agent") == null) {
            builder.header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            )
        }

        // 告诉它我想要 json 格式
        builder.header("Accept", "application/json")

        // 核心：如果有 COOKIE，就带上；没有就不带
        if (userCookie.isNotEmpty()) {
//            builder.header("Cookie", "COOKIE=$userCookie;")
            val cleanCookie = userCookie.replace("\n", "").replace("\r", "").trim()
            builder.header("Cookie", cleanCookie)
        }

        chain.proceed(builder.build())
    }

    // 创建一个 HttpClient，把监控器装进去
    private val client = OkHttpClient.Builder()
        .addInterceptor(headerInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    // 创建一个 Retrofit 实例
    val service: BLiveApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()) // 告诉它怎么解析 JSON
            .build()
            .create(BLiveApiService::class.java)
    }
}