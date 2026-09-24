package com.billarlegends.portfolio.data.remote.binance

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

/**
 * Header interno (nunca viaja a Binance) que le indica a [BinanceSigningInterceptor] que la
 * petición debe firmarse. Debe coincidir con [BinanceSigningInterceptor.SIGN_HEADER].
 */
private const val NEEDS_SIGNATURE = "X-Portfolio-Needs-Signature: true"

interface BinanceApi {

    @GET("api/v3/account")
    @Headers([NEEDS_SIGNATURE])
    suspend fun getAccount(): BinanceAccountResponse

    @GET("sapi/v1/account/apiRestrictions")
    @Headers([NEEDS_SIGNATURE])
    suspend fun getApiRestrictions(): BinanceApiRestrictions

    /** Sin parámetros devuelve el precio de todos los símbolos; endpoint público, no se firma. */
    @GET("api/v3/ticker/price")
    suspend fun getAllPrices(): List<BinanceTickerPrice>

    @GET("sapi/v1/capital/deposit/hisrec")
    @Headers([NEEDS_SIGNATURE])
    suspend fun getDepositHistory(
        @Query("limit") limit: Int = 1000,
    ): List<BinanceDepositRecord>

    @GET("sapi/v1/capital/withdraw/history")
    @Headers([NEEDS_SIGNATURE])
    suspend fun getWithdrawHistory(
        @Query("limit") limit: Int = 1000,
    ): List<BinanceWithdrawRecord>

    @GET("api/v3/myTrades")
    @Headers([NEEDS_SIGNATURE])
    suspend fun getMyTrades(
        @Query("symbol") symbol: String,
        @Query("limit") limit: Int = 500,
    ): List<BinanceMyTrade>

    companion object {
        const val BASE_URL = "https://api.binance.com/"
    }
}
