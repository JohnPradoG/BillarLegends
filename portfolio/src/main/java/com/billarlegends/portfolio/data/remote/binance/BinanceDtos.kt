package com.billarlegends.portfolio.data.remote.binance

import kotlinx.serialization.Serializable

@Serializable
data class BinanceAccountResponse(
    val canTrade: Boolean = false,
    val canWithdraw: Boolean = false,
    val canDeposit: Boolean = false,
    val balances: List<BinanceBalance> = emptyList(),
)

@Serializable
data class BinanceBalance(
    val asset: String,
    val free: String,
    val locked: String,
)

@Serializable
data class BinanceApiRestrictions(
    val ipRestrict: Boolean = false,
    val enableReading: Boolean = false,
    val enableWithdrawals: Boolean = false,
    val enableInternalTransfer: Boolean = false,
    val enableSpotAndMarginTrading: Boolean = false,
    val enableFutures: Boolean = false,
    val enableMargin: Boolean = false,
)

@Serializable
data class BinanceTickerPrice(
    val symbol: String,
    val price: String,
)

@Serializable
data class BinanceDepositRecord(
    val id: String = "",
    val amount: String,
    val coin: String,
    val status: Int,
    val txId: String? = null,
    val insertTime: Long,
    val network: String = "",
)

@Serializable
data class BinanceWithdrawRecord(
    val id: String,
    val amount: String,
    val transactionFee: String = "0",
    val coin: String,
    val status: Int,
    val txId: String? = null,
    /** Formato "yyyy-MM-dd HH:mm:ss" (UTC). */
    val applyTime: String,
    val network: String = "",
)

@Serializable
data class BinanceMyTrade(
    val symbol: String,
    val id: Long,
    val orderId: Long,
    val price: String,
    val qty: String,
    val quoteQty: String,
    val commission: String,
    val commissionAsset: String,
    val time: Long,
    val isBuyer: Boolean,
)
