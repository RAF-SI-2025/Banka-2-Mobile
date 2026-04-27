package rs.raf.banka2.mobile.data.dto.account

import com.squareup.moshi.JsonClass

/**
 * Mapira `AccountResponseDto` sa backenda. Sva opciona polja su nullable
 * jer Spring Page response ponekad propusti polja kod legacy zapisa.
 */
@JsonClass(generateAdapter = true)
data class AccountDto(
    val id: Long,
    val accountNumber: String,
    val name: String? = null,
    val accountType: String? = null,
    val accountSubtype: String? = null,
    val currency: String? = null,
    val balance: Double = 0.0,
    val availableBalance: Double = 0.0,
    val reservedAmount: Double? = null,
    val status: String? = null,
    val dailyLimit: Double? = null,
    val monthlyLimit: Double? = null,
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
    val initialDeposit: Double = 0.0,
    val ownerEmail: String,
    val createCard: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AccountNameUpdateDto(
    val name: String
)

@JsonClass(generateAdapter = true)
data class AccountLimitsUpdateDto(
    val dailyLimit: Double? = null,
    val monthlyLimit: Double? = null,
    val otpCode: String? = null
)

@JsonClass(generateAdapter = true)
data class AccountRequestDto(
    val accountType: String,
    val accountSubtype: String? = null,
    val currency: String,
    val initialDeposit: Double = 0.0,
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
    val initialDeposit: Double? = null,
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
