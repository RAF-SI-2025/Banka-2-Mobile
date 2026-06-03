package rs.raf.banka2.mobile.data.dto.transfer

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * ME-11: novcana polja prebacena sa Double na BigDecimal (spec C2 §255).
 *
 * P1-mobile-banking-1 (R1-125): KONTRAKT uskladjen sa BE. BE `TransferInternalRequestDto`
 * (`rs.raf.banka2_bek.transfers.dto.TransferInternalRequestDto`) trazi
 * `fromAccountNumber`/`toAccountNumber` (Stringovi, @NotBlank) + `amount` + `otpCode`,
 * i NEMA `fromAccountId`/`toAccountId`/`currency`/`description`. Stari Mobile DTO je
 * slao `fromAccountId` (Long) → BE @NotBlank na `fromAccountNumber` → svaki transfer 400.
 */
@JsonClass(generateAdapter = true)
data class TransferInternalRequestDto(
    val fromAccountNumber: String,
    val toAccountNumber: String,
    val amount: BigDecimal,
    val otpCode: String
)

/**
 * P1-mobile-banking-1 (R1-125): BE `TransferFxRequestDto` ima ISTU strukturu kao
 * internal (`fromAccountNumber`/`toAccountNumber`/`amount`/`otpCode`) — NEMA `currency`
 * polje (valuta se izvodi sa racuna na BE-u). Stari Mobile FX DTO je slao `*Id` + `currency`.
 */
@JsonClass(generateAdapter = true)
data class TransferFxRequestDto(
    val fromAccountNumber: String,
    val toAccountNumber: String,
    val amount: BigDecimal,
    val otpCode: String
)

/**
 * P1-mobile-banking-1 (R1-126): KONTRAKT uskladjen sa BE `TransferResponseDto`.
 * BE salje `fromAccountNumber`/`toAccountNumber`/`toAmount`/`fromCurrency`/`exchangeRate`/
 * `commission` — stari Mobile DTO je citao `fromAccount`/`toAccount`/`convertedAmount`/
 * `currency`/`rate`/`fee` → istorija je prikazivala "?→?" sa iznosom 0. Zadrzavamo
 * Kotlin imena (koja screen vec cita) ali ih vezujemo za BE JSON imena preko [Json] aliasa.
 */
@JsonClass(generateAdapter = true)
data class TransferResponseDto(
    val id: Long,
    val orderNumber: String? = null,
    @param:Json(name = "fromAccountNumber") val fromAccount: String? = null,
    @param:Json(name = "toAccountNumber") val toAccount: String? = null,
    val amount: BigDecimal = BigDecimal.ZERO,
    @param:Json(name = "fromCurrency") val currency: String? = null,
    @param:Json(name = "toAmount") val convertedAmount: BigDecimal? = null,
    @param:Json(name = "toCurrency") val convertedCurrency: String? = null,
    @param:Json(name = "exchangeRate") val rate: BigDecimal? = null,
    @param:Json(name = "commission") val fee: BigDecimal? = null,
    val clientFirstName: String? = null,
    val clientLastName: String? = null,
    val status: String? = null,
    val createdAt: String? = null
)
