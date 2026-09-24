package com.billarlegends.portfolio.data.remote.exness

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ExnessBridgeApi {

    @GET("account")
    suspend fun getAccount(@Header("Authorization") bearerToken: String): ExnessAccountResponse

    @GET("history")
    suspend fun getHistory(
        @Header("Authorization") bearerToken: String,
        @Query("from") fromMillis: Long = 0,
    ): List<ExnessDeal>
}
