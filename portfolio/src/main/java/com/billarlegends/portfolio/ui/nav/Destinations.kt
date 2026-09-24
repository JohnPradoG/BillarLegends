package com.billarlegends.portfolio.ui.nav

object Destinations {
    const val DASHBOARD = "dashboard"
    const val ACCOUNTS = "accounts"
    const val ADD_BINANCE_ACCOUNT = "accounts/add-binance"
    const val ADD_EXNESS_CONNECTION = "accounts/add-exness"
    const val BUSINESSES = "businesses"
    const val BUSINESS_DETAIL = "businesses/{businessId}"

    fun businessDetail(businessId: Long) = "businesses/$businessId"
}
