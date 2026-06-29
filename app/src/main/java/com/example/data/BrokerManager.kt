package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class PaperPosition(
    val instrument: String,
    val direction: String,
    val entryPrice: Double,
    val units: Double,
    val date: Long
)

class BrokerManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("broker_prefs", Context.MODE_PRIVATE)
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val positionListAdapter = moshi.adapter<List<PaperPosition>>(
        Types.newParameterizedType(List::class.java, PaperPosition::class.java)
    )

    // --- OANDA Config ---
    fun getOandaToken(): String? = prefs.getString("oanda_token", null)
    fun getOandaAccountId(): String? = prefs.getString("oanda_account_id", null)
    fun getOandaEnvironment(): String = prefs.getString("oanda_env", "sandbox") ?: "sandbox"
    fun isOandaConnected(): Boolean = getOandaToken() != null && getOandaAccountId() != null

    fun saveOandaCredentials(token: String, accountId: String, environment: String) {
        prefs.edit()
            .putString("oanda_token", token)
            .putString("oanda_account_id", accountId)
            .putString("oanda_env", environment)
            .putBoolean("is_paper_mode", false)
            .apply()
    }

    fun logoutOanda() {
        prefs.edit()
            .remove("oanda_token")
            .remove("oanda_account_id")
            .remove("oanda_env")
            .apply()
    }

    // --- Mode Control ---
    fun isPaperMode(): Boolean = prefs.getBoolean("is_paper_mode", true)
    
    fun setPaperMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_paper_mode", enabled).apply()
    }

    // --- Paper Trading State ---
    fun getPaperBalance(): Double = prefs.getFloat("paper_balance", 100000f).toDouble()
    
    private fun savePaperBalance(balance: Double) {
        prefs.edit().putFloat("paper_balance", balance.toFloat()).apply()
    }

    fun getPaperPositions(): List<PaperPosition> {
        val json = prefs.getString("paper_positions", null) ?: return emptyList()
        return try {
            positionListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun savePaperPositions(positions: List<PaperPosition>) {
        val json = positionListAdapter.toJson(positions)
        prefs.edit().putString("paper_positions", json).apply()
    }

    fun clearPaperAccount() {
        prefs.edit()
            .putFloat("paper_balance", 100000f)
            .remove("paper_positions")
            .apply()
    }

    // --- Retrofit API Client ---
    private fun getRetrofit(): Retrofit {
        val env = getOandaEnvironment()
        val baseUrl = when (env) {
            "practice" -> "https://api-fxpractice.oanda.com/"
            "live" -> "https://api-fxtrade.oanda.com/"
            else -> "https://api-sandbox.oanda.com/"
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private fun getService(): BrokerService {
        return getRetrofit().create(BrokerService::class.java)
    }

    // --- API Calls ---
    suspend fun validateAndConnectOanda(token: String, accountId: String, environment: String): Result<OandaAccountSummary> {
        return try {
            val authHeader = "Bearer $token"
            // Instantiating temporary Retrofit with specified environment
            val baseUrl = when (environment) {
                "practice" -> "https://api-fxpractice.oanda.com/"
                "live" -> "https://api-fxtrade.oanda.com/"
                else -> "https://api-sandbox.oanda.com/"
            }
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .build()
            val tempRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            val service = tempRetrofit.create(BrokerService::class.java)
            val response = service.getAccountSummary(authHeader, accountId)
            
            // Save if successful
            saveOandaCredentials(token, accountId, environment)
            Result.success(response.account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOandaSummary(): Result<OandaAccountSummary> {
        val token = getOandaToken() ?: return Result.failure(IllegalStateException("Not logged in to OANDA"))
        val accountId = getOandaAccountId() ?: return Result.failure(IllegalStateException("Account ID missing"))
        return try {
            val response = getService().getAccountSummary("Bearer $token", accountId)
            Result.success(response.account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun placeOandaOrder(instrument: String, units: Double): Result<OandaOrderFillTransaction> {
        val token = getOandaToken() ?: return Result.failure(IllegalStateException("Not logged in to OANDA"))
        val accountId = getOandaAccountId() ?: return Result.failure(IllegalStateException("Account ID missing"))
        return try {
            val authHeader = "Bearer $token"
            val request = OandaOrderRequest(
                order = OandaOrderDetails(
                    units = units.toInt().toString(),
                    instrument = instrument.replace("/", "_").uppercase()
                )
            )
            val response = getService().createOrder(authHeader, accountId, request)
            val fill = response.orderFillTransaction
            if (fill != null) {
                Result.success(fill)
            } else {
                Result.failure(Exception(response.errorMessage ?: "Order execution failed or was canceled."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Paper Order Execution ---
    fun placePaperOrder(instrument: String, direction: String, lotSize: Double, executionPrice: Double): Result<PaperPosition> {
        val currentBalance = getPaperBalance()
        
        // 1 lot of Forex = 100,000 units
        // Gold 1 lot = 100 units
        val unitsMultiplier = if (instrument.contains("XAU") || instrument.contains("GOLD")) 100.0 else 100000.0
        val units = lotSize * unitsMultiplier
        
        val signedUnits = if (direction.uppercase() == "LONG") units else -units
        
        // Check margin or simple balance requirements (e.g. must have positive balance)
        if (currentBalance <= 0.0) {
            return Result.failure(IllegalStateException("Insufficient funds in Paper Account!"))
        }

        val positions = getPaperPositions().toMutableList()
        
        // Simple net-out or hedging system: check if there's an opposite open position for the same instrument
        val oppositeDirection = if (direction.uppercase() == "LONG") "SHORT" else "LONG"
        val existingOppositeIdx = positions.indexOfFirst { it.instrument == instrument && it.direction == oppositeDirection }
        
        if (existingOppositeIdx != -1) {
            // Close the opposite position and calculate P&L!
            val closedPos = positions.removeAt(existingOppositeIdx)
            val pipMultiplier = if (instrument.uppercase().contains("JPY")) 1000.0 else if (instrument.uppercase().contains("XAU")) 10.0 else 100000.0
            
            val entry = closedPos.entryPrice
            val exit = executionPrice
            val pnlPoints = if (closedPos.direction == "LONG") (exit - entry) else (entry - exit)
            val realizedPnl = pnlPoints * closedPos.units // units is lotSize * multiplier, so pip multiplier is already scaled
            
            val newBalance = currentBalance + (pnlPoints * closedPos.units) // simplified Pnl
            savePaperBalance(newBalance)
            savePaperPositions(positions)
            
            return Result.failure(Exception("Closed open opposite $oppositeDirection position for $instrument. Net P&L realized: $${String.format("%.2f", realizedPnl * 0.1)}"))
        }

        // Add new position
        val newPos = PaperPosition(
            instrument = instrument,
            direction = direction.uppercase(),
            entryPrice = executionPrice,
            units = units,
            date = System.currentTimeMillis()
        )
        positions.add(newPos)
        savePaperPositions(positions)
        
        return Result.success(newPos)
    }

    // Close specific paper position
    fun closePaperPosition(index: Int, currentPrice: Double): Double {
        val positions = getPaperPositions().toMutableList()
        if (index < 0 || index >= positions.size) return 0.0
        
        val pos = positions.removeAt(index)
        val entry = pos.entryPrice
        val exit = currentPrice
        val pnlPoints = if (pos.direction == "LONG") (exit - entry) else (entry - exit)
        
        // Calculate realized profit/loss
        // Pnl in USD = pnlPoints * units * pointValue scaling
        // Let's use simple scaling: 
        val profitMultiplier = if (pos.instrument.uppercase().contains("JPY")) 0.01 else if (pos.instrument.uppercase().contains("XAU")) 1.0 else 1.0
        val pnl = pnlPoints * pos.units * profitMultiplier * 0.1 // Scaled to reasonable size
        
        val newBalance = getPaperBalance() + pnl
        savePaperBalance(newBalance)
        savePaperPositions(positions)
        
        return pnl
    }
}
