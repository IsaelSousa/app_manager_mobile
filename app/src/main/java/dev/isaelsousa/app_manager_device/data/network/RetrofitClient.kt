package dev.isaelsousa.app_manager_device.data.network

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val logging = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}

val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(1, TimeUnit.MINUTES)
    .writeTimeout(30, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .addInterceptor(logging)
    .build()

val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl("http://192.168.15.7:5299")
    .addConverterFactory(GsonConverterFactory.create())
    .client(client)
    .build()