package rs.raf.banka2.mobile.data.dto.margin

import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class MarginAccountDto(
    val id: Long,
    val accountNumber: String? = null,
    val initialMargin: BigDecimal = BigDecimal.ZERO,
    val maintenanceMargin: BigDecimal = BigDecimal.ZERO,
    val loanValue: BigDecimal = BigDecimal.ZERO,
    val bankParticipation: BigDecimal? = null,
    val currency: String? = null,
    val active: Boolean = true,
    val status: String? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class MarginTransactionDto(
    val id: Long,
    val type: String? = null,
    val amount: BigDecimal = BigDecimal.ZERO,
    val timestamp: String? = null,
    val balanceAfter: BigDecimal? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateMarginAccountDto(
    // BE `CreateMarginAccountDto.accountId` je @NotNull — bez njega BE vraca 400
    // ("ID racuna je obavezan"). `createForUser` resolvuje vlasnika iz JWT-a i
    // VEZUJE marzni racun za ovaj bazni RSD racun (skida initialDeposit s njega).
    val accountId: Long,
    val initialMargin: BigDecimal,
    val maintenanceMargin: BigDecimal,
    val bankParticipation: BigDecimal,
    val userId: Long? = null,
    val companyId: Long? = null
)

@JsonClass(generateAdapter = true)
data class MarginAmountRequestDto(
    val amount: BigDecimal
)

/**
 * Odgovor BE `POST /margin-accounts/{id}/deposit|withdraw` — kontroler vraca
 * `Map<String,String>` oblika `{"message":"Deposit successful"}`, NE `MarginAccountDto`.
 * Ranije je Mobile ocekivao `MarginAccountDto` pa je Moshi parser padao na
 * uspesnom 200 odgovoru → korisnik bi video gresku iako je BE uspeo (R1-269/270).
 */
@JsonClass(generateAdapter = true)
data class MarginMessageDto(
    val message: String? = null
)
