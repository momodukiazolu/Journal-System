package com.example.data

import retrofit2.http.*

interface BrokerService {
    @GET("v3/accounts/{accountID}/summary")
    suspend fun getAccountSummary(
        @Header("Authorization") authHeader: String,
        @Path("accountID") accountID: String
    ): OandaAccountSummaryResponse

    @POST("v3/accounts/{accountID}/orders")
    suspend fun createOrder(
        @Header("Authorization") authHeader: String,
        @Path("accountID") accountID: String,
        @Body request: OandaOrderRequest
    ): OandaOrderResponse
}
