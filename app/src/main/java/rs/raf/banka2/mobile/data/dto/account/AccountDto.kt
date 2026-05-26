package rs.raf.banka2.mobile.data.dto.account

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Mapira `AccountResponseDto` sa backenda. Sva opciona polja su nullable
 * jer Spring Page response ponekad propusti polja kod legacy zapisa.
 *
 * ME-11: novcana polja prebacena sa Double na BigDecimal — Moshi
 * KotlinJsonAdapterFactory podrzava BigDecimal nativno (vec se koristi u
 * CardDto.kt i SavingsDto.kt). Spec C2 §255 zahteva precision aritmetika.
 */
@JsonClass(generateAdapter = true)
data class AccountDto(
    val id: Long,
    val accountNumber: String,
    val name: String? = null,
    val accountType: String? = null,
    val accountSubtype: String? = null,
    val currency: String? = null,
    val balance: BigDecimal = BigDecimal.ZERO,
    val availableBalance: BigDecimal = BigDecimal.ZERO,
    val reservedAmount: BigDecimal? = null,
    val status: String? = null,
    val dailyLimit: BigDecimal? = null,
    val monthlyLimit: BigDecimal? = null,
    val createdAt: String? = null,
    val ownerEmail: String? = null,
    val ownerName: String? = null,
    val companyName: String? = null,
    val companyRegistrationNumber: String? = null,
    val activityCode: String? = null,           // sifra delatnosti xx.xx
    val taxNumber: String? = null,
    val authorizedPersons: List<AuthorizedPersonDto> = emptyList(),
    val accountCategory: String? = null
) {
    val isBusiness: Boolean
        get() = accountType.equals("BUSINESS", true) || !companyName.isNullOrBlank()

    /**
     * ME-11 helper: izracunava rezervisani iznos koji ide ka UI prikazu.
     * Prefer-ira `reservedAmount` polje iz BE; fallback na (balance - available).
     */
    val effectiveReserved: BigDecimal
        get() = reservedAmount ?: (balance - availableBalance)
}

@JsonClass(generateAdapter = true)
data class AuthorizedPersonDto(
    val id: Long? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateAccountDto(
    val accountType: String,
    val accountSubtype: String? = null,
    val currency: String,
    val initialDeposit: BigDecimal = BigDecimal.ZERO,
    val ownerEmail: String,
    val createCard: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AccountNameUpdateDto(
    val name: String
)

@JsonClass(generateAdapter = true)
data class AccountLimitsUpdateDto(
    val dailyLimit: BigDecimal? = null,
    val monthlyLimit: BigDecimal? = null,
    val otpCode: String? = null
)

@JsonClass(generateAdapter = true)
data class AccountRequestDto(
    val accountType: String,
    val accountSubtype: String? = null,
    val currency: String,
    val initialDeposit: BigDecimal = BigDecimal.ZERO,
    val createCard: Boolean = false,
    val note: String? = null
)

@JsonClass(generateAdapter = true)
data class AccountRequestResponseDto(
    val id: Long,
    val clientEmail: String? = null,
    val clientName: String? = null,
    val accountType: String? = null,
    val accountSubtype: String? = null,
    val currency: String? = null,
    val initialDeposit: BigDecimal? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val rejectionReason: String? = null,
    val note: String? = null
)

@JsonClass(generateAdapter = true)
data class AccountRequestRejectDto(
    val reason: String
)

@JsonClass(generateAdapter = true)
data class AccountStatusUpdateDto(
    val status: String
)
