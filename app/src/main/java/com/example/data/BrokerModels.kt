package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OandaAccountSummaryResponse(
    @Json(name = "account") val account: OandaAccountSummary
)

@JsonClass(generateAdapter = true)
data class OandaAccountSummary(
    @Json(name = "balance") val balance: String,
    @Json(name = "currency") val currency: String,
    @Json(name = "openPositionCount") val openPositionCount: Int,
    @Json(name = "NAV") val nav: String
)

@JsonClass(generateAdapter = true)
data class OandaOrderRequest(
    @Json(name = "order") val order: OandaOrderDetails
)

@JsonClass(generateAdapter = true)
data class OandaOrderDetails(
    @Json(name = "units") val units: String,
    @Json(name = "instrument") val instrument: String,
    @Json(name = "timeInForce") val timeInForce: String = "FOK",
    @Json(name = "type") val type: String = "MARKET",
    @Json(name = "positionFill") val positionFill: String = "DEFAULT"
)

@JsonClass(generateAdapter = true)
data class OandaOrderResponse(
    @Json(name = "orderFillTransaction") val orderFillTransaction: OandaOrderFillTransaction? = null,
    @Json(name = "errorMessage") val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class OandaOrderFillTransaction(
    @Json(name = "id") val id: String,
    @Json(name = "price") val price: String,
    @Json(name = "requestedUnits") val requestedUnits: String,
    @Json(name = "filledUnits") val filledUnits: String
)
