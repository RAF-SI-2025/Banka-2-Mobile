package rs.raf.banka2.mobile.data.dto.card

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * ME-03 fix: paritet sa BE `CardResponseDto.java` posle 14.05.2026 vece-3 nadogradnje.
 * Brend (`cardType` = VISA/MASTERCARD/DINACARD/AMEX) je razdvojen od kategorije
 * placanja (`cardCategory` = DEBIT/CREDIT/INTERNET_PREPAID).
 *
 * ME-11 fix: novcana polja prebacena sa Double na BigDecimal — Moshi
 * KotlinJsonAdapterFactory podrzava BigDecimal nativno (vec se koristi u
 * SavingsDto.kt). Spec C2 §255 zahteva precision aritmetika.
 */
@JsonClass(generateAdapter = true)
data class CardDto(
    val id: Long,
    val cardNumber: String? = null,
    val cardName: String? = null,
    val cardType: String? = null,        // brend: VISA / MASTERCARD / DINACARD / AMEX
    val cardCategory: String? = null,    // ME-03: DEBIT / CREDIT / INTERNET_PREPAID
    val brand: String? = null,
    val status: String? = null,
    val cardLimit: BigDecimal? = null,
    val balance: BigDecimal? = null,
    val prepaidBalance: BigDecimal? = null,    // ME-03: balance na INTERNET_PREPAID kartici
    val creditLimit: BigDecimal? = null,       // ME-03: maksimalni iznos za CREDIT
    val outstandingBalance: BigDecimal? = null, // ME-03: trenutno duguje banci (CREDIT)
    val cvv: String? = null,
    val pin: String? = null,
    val expirationDate: String? = null,
    val accountId: Long? = null,
    val accountNumber: String? = null,
    val ownerName: String? = null
) {
    val isPrepaid: Boolean get() = cardCategory.equals("INTERNET_PREPAID", ignoreCase = true)
    val isCredit: Boolean get() = cardCategory.equals("CREDIT", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class CardLimitUpdateDto(
    val cardLimit: BigDecimal
)

/**
 * ME-03 fix: card request prosiren sa `cardCategory` i `creditLimit` (samo za CREDIT karticu).
 */
@JsonClass(generateAdapter = true)
data class CardRequestCreateDto(
    val accountId: Long,
    val cardLimit: BigDecimal,
    val cardType: String,                        // brend (VISA/MC/...)
    val cardCategory: String = "DEBIT",          // ME-03: DEBIT/CREDIT/INTERNET_PREPAID
    val creditLimit: BigDecimal? = null          // ME-03: za CREDIT kategoriju
)

@JsonClass(generateAdapter = true)
data class CardRequestResponseDto(
    val id: Long,
    val accountId: Long? = null,
    val accountNumber: String? = null,
    val ownerName: String? = null,
    val cardLimit: BigDecimal? = null,
    val cardType: String? = null,
    val cardCategory: String? = null,
    val creditLimit: BigDecimal? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val rejectionReason: String? = null
)

@JsonClass(generateAdapter = true)
data class CardRequestRejectDto(
    val reason: String
)

/**
 * ME-03: payload za POST /cards/{id}/top-up + /withdraw. BE prima Map<String,Object>
 * pa Moshi serijalizuje BigDecimal kao number/string — BE Jackson prepoznaje oba.
 */
@JsonClass(generateAdapter = true)
data class CardTopUpRequest(
    val sourceAccountId: Long,
    val amount: BigDecimal
)

@JsonClass(generateAdapter = true)
data class CardWithdrawRequest(
    val targetAccountId: Long,
    val amount: BigDecimal
)
