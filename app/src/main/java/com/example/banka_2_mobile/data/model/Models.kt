package com.example.banka_2_mobile.data.model

import com.google.gson.annotations.SerializedName

// ─── Auth Models ───────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("refreshToken")
    val refreshToken: String
)

data class RefreshRequest(
    val refreshToken: String
)

data class OtpResponse(
    val active: Boolean,
    val code: String? = null,
    val expiresInSeconds: Int? = null,
    val attempts: Int? = null,
    val maxAttempts: Int? = null,
    val message: String? = null
)

// ─── Account Models ────────────────────────────────────────

data class Account(
    val id: Long,
    val accountNumber: String,
    val name: String? = null,
    val accountType: String? = null,
    val subType: String? = null,
    val currency: String? = null,
    val balance: Double = 0.0,
    val availableBalance: Double = 0.0,
    val status: String? = null,
    val dailyLimit: Double? = null,
    val monthlyLimit: Double? = null,
    val createdAt: String? = null,
    val ownerName: String? = null,
    val companyName: String? = null
)

// ─── Payment / Transaction Models ──────────────────────────

data class Payment(
    val id: Long,
    val orderNumber: String? = null,
    val fromAccount: String? = null,
    val toAccount: String? = null,
    val amount: Double = 0.0,
    val currency: String? = null,
    val status: String? = null,
    val direction: String? = null,
    val description: String? = null,
    val recipientName: String? = null,
    val createdAt: String? = null,
    val fee: Double? = null,
    val paymentCode: String? = null,
    val referenceNumber: String? = null
)

data class PaginatedResponse<T>(
    val content: List<T> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
    val size: Int = 0
)

// ─── Card Models ───────────────────────────────────────────

data class CardResponse(
    val id: Long,
    val cardNumber: String? = null,
    val cardType: String? = null,
    val status: String? = null,
    val cardLimit: Double? = null,
    val accountId: Long? = null,
    val accountNumber: String? = null,
    val cardName: String? = null,
    val expirationDate: String? = null,
    val ownerName: String? = null,
    val cvv: String? = null
)

// ─── Exchange Models ───────────────────────────────────────

data class ExchangeRate(
    val currency: String,
    val rate: Double? = null,
    val buyRate: Double? = null,
    val sellRate: Double? = null,
    val middleRate: Double? = null,
    val date: String? = null
)

data class CalculateExchangeResponse(
    val convertedAmount: Double,
    val exchangeRate: Double,
    val fromCurrency: String,
    val toCurrency: String
)

// ─── Celina 3: Berza / Securities Models ──────────────────

// TODO: Verify all field names against backend ListingDto once endpoints are ready.
//       Backend repo: RAF-SI-2025/Banka-2-Backend — check listing-service DTOs.
//       Fields below are based on the project specification (Celina 3).

data class Listing(
    val id: Long,
    val ticker: String,
    val name: String,
    val exchangeAcronym: String? = null,
    val listingType: String,           // "STOCK", "FUTURES", "FOREX"
    val price: Double,
    val ask: Double? = null,
    val bid: Double? = null,
    val volume: Long? = null,
    val priceChange: Double? = null,    // absolute change
    val changePercent: Double? = null,  // percentage change (e.g. -2.35)
    val high: Double? = null,
    val low: Double? = null,
    val marketCap: Long? = null,       // stocks only
    val outstandingShares: Long? = null,// stocks only
    val dividendYield: Double? = null,  // stocks only
    val contractSize: Int? = null,      // futures only
    val contractUnit: String? = null,   // futures only
    val maintenanceMargin: Double? = null, // futures only
    val settlementDate: String? = null  // futures only
)

data class ListingDailyPrice(
    val date: String,
    val price: Double,
    val high: Double,
    val low: Double,
    val volume: Long
)

// TODO: Backend may return Spring Page<Listing> — confirm field names
//       match PaginatedResponse<T> or if a separate DTO is used.
//       If identical to PaginatedResponse, reuse that generic class instead.
data class PaginatedListingResponse(
    val content: List<Listing> = emptyList(),
    val totalPages: Int = 0,
    val totalElements: Long = 0,
    val number: Int = 0,
    val size: Int = 20
)

data class PortfolioItem(
    val id: Long,
    val listingTicker: String,
    val listingName: String? = null,
    val listingType: String? = null,       // "STOCK", "FUTURES"
    val quantity: Int,
    val averageBuyPrice: Double,
    val currentPrice: Double? = null,
    val profit: Double? = null,
    val profitPercent: Double? = null,
    val publicQuantity: Int? = null,        // quantity visible to other users
    val inOrderQuantity: Int? = null        // quantity locked in pending orders
)

data class PortfolioSummary(
    val totalValue: Double,
    val totalProfit: Double,
    val totalProfitPercent: Double? = null,
    val taxOwed: Double? = null            // porez na kapitalnu dobit
)

// TODO: Confirm order types and directions with backend.
//       Spec says: orderType = MARKET | LIMIT | STOP | STOP_LIMIT
//       direction = BUY | SELL
//       allOrNone = boolean (AON flag)
//       Backend may also require accountId for settlement.
data class CreateOrderRequest(
    val listingId: Long,
    val orderType: String,      // "MARKET", "LIMIT", "STOP", "STOP_LIMIT"
    val quantity: Int,
    val direction: String,      // "BUY", "SELL"
    val limitPrice: Double? = null,
    val stopPrice: Double? = null,
    val allOrNone: Boolean = false,
    val accountId: Long? = null  // settlement account
)

data class OrderResponse(
    val id: Long,
    val listingId: Long? = null,
    val listingTicker: String? = null,
    val listingName: String? = null,
    val orderType: String,           // "MARKET", "LIMIT", "STOP", "STOP_LIMIT"
    val direction: String,           // "BUY", "SELL"
    val quantity: Int,
    val filledQuantity: Int? = null,
    val limitPrice: Double? = null,
    val stopPrice: Double? = null,
    val status: String,              // "PENDING", "APPROVED", "DONE", "DECLINED", "CANCELLED"
    val allOrNone: Boolean? = null,
    val fee: Double? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
