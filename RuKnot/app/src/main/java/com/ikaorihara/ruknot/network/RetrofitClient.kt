package com.ikaorihara.ruknot.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.live.bilibili.com"

    // 创建一个监控器 (Interceptor)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // 设置级别为 BODY，意味着我们会打印所有的请求头和返回内容
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 伪装拦截器：把自己伪装成浏览器
    private val headerInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            // 这一行是关键！告诉B站：我是电脑版的 Edge/Chrome 浏览器，不是机器人
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            // 顺便告诉它我想要 json 格式
            .header("Accept", "application/json")
            .build()
        chain.proceed(newRequest)
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