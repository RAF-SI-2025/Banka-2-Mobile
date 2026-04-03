package com.example.banka_2_mobile.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.model.Account
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.SuccessGreen
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import com.example.banka_2_mobile.ui.theme.WarningYellow
import com.example.banka_2_mobile.ui.theme.ErrorRed
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val email = authRepository.getEmail() ?: ""
    val displayName = extractNameFromEmail(email)

    suspend fun fetchAccounts() {
        try {
            val response = RetrofitClient.api.getMyAccounts()
            if (response.isSuccessful) {
                accounts = response.body() ?: emptyList()
            } else if (response.code() == 401) {
                authRepository.clearTokens()
                onLogout()
                return
            } else {
                errorMessage = "Greska pri ucitavanju naloga (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Greska u mrezi. Proverite konekciju."
        }
        isLoading = false
        isRefreshing = false
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    LaunchedEffect(Unit) {
        fetchAccounts()
    }

    // Background animation
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val orbAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch { fetchAccounts() }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg)
            ) {
                // Background orbs
                Box(
                    modifier = Modifier
                        .size(350.dp)
                        .alpha(orbAlpha)
                        .align(Alignment.TopEnd)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Indigo500.copy(alpha = 0.3f), Color.Transparent),
                                radius = 500f
                            )
                        )
                )

                // Secondary orb bottom-left
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .alpha(orbAlpha * 0.6f)
                        .align(Alignment.BottomStart)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Violet600.copy(alpha = 0.2f), Color.Transparent),
                                radius = 400f
                            )
                        )
                )

                if (isLoading) {
                    LoadingShimmer()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ─── Header: Greeting + Date ───────────────────
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column {
                                Text(
                                    text = "Dobrodosli, $displayName \uD83D\uDC4B",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formattedDate(),
                                    fontSize = 14.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        // ─── Total Balance Hero Card ───────────────────
                        item {
                            TotalBalanceCard(accounts)
                        }

                        // ─── Quick Actions Row ─────────────────────────
                        item {
                            QuickActionsRow()
                        }

                        // ─── Section Title: Moji racuni ────────────────
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(22.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Indigo500, Violet600)
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Moji racuni",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        // ─── Account Cards or Empty State ──────────────
                        if (accounts.isEmpty()) {
                            item {
                                EmptyAccountsState()
                            }
                        } else {
                            items(accounts) { account ->
                                AccountCard(account)
                            }
                        }

                        // Bottom spacing for navbar
                        item {
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ─── Total Balance Hero Card ─────────────────────────────────

@Composable
private fun TotalBalanceCard(accounts: List<Account>) {
    val rsdAccounts = accounts.filter { (it.currency ?: "RSD").uppercase() == "RSD" }
    val totalRsd = rsdAccounts.sumOf { it.balance }
    val formattedBalance = formatCurrency(totalRsd, "RSD")

    // Shimmer animation
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Indigo500, Violet600),
                    start = Offset(0f, 0f),
                    end = Offset(800f, 800f)
                )
            )
    ) {
        // Shimmer overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.08f)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White,
                            Color.Transparent
                        ),
                        start = Offset(shimmerOffset * 600f, 0f),
                        end = Offset(shimmerOffset * 600f + 300f, 300f)
                    )
                )
        )

        // Decorative circle top-right
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .alpha(0.08f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        radius = 160f
                    )
                )
        )

        // Decorative circle bottom-left
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.BottomStart)
                .alpha(0.06f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        radius = 120f
                    )
                )
        )

        Column(
            modifier = Modifier.padding(28.dp)
        ) {
            Text(
                text = "Ukupno stanje",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = formattedBalance,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Default
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${accounts.size} ${accountCountLabel(accounts.size)}",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ─── Quick Actions Row ───────────────────────────────────────

private data class QuickAction(
    val label: String,
    val icon: ImageVector
)

@Composable
private fun QuickActionsRow() {
    val actions = listOf(
        QuickAction("Placanja", Icons.AutoMirrored.Filled.Send),
        QuickAction("Prenos", Icons.Filled.SwapHoriz),
        QuickAction("Menjacnica", Icons.Filled.CurrencyExchange),
        QuickAction("Kartice", Icons.Filled.CreditCard)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        actions.forEach { action ->
            QuickActionButton(action)
        }
    }
}

@Composable
private fun QuickActionButton(action: QuickAction) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(DarkCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = Indigo400,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = action.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted
        )
    }
}

// ─── Account Card ────────────────────────────────────────────

@Composable
private fun AccountCard(account: Account) {
    val currency = (account.currency ?: "RSD").uppercase()
    val gradient = currencyGradient(currency)
    val typeBadge = accountTypeBadge(account.accountType)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkCard)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Currency badge circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(brush = Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currencySymbol(currency),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Center: Name, number, type badge
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = account.name ?: account.accountType ?: "Racun",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = formatAccountNumber(account.accountNumber),
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    if (typeBadge.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Indigo500.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = typeBadge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Indigo400
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right: Balance
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatCurrency(account.balance, currency),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    if (account.availableBalance != account.balance) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Raspolozivo: ${formatCurrency(account.availableBalance, currency)}",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Bottom gradient line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                gradient[0].copy(alpha = 0.4f),
                                gradient[1].copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

// ─── Empty State ─────────────────────────────────────────────

@Composable
private fun EmptyAccountsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(DarkCard),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\uD83C\uDFE6",
                fontSize = 32.sp
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Nemate racune",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Trenutno nemate nijedan aktivan racun",
            fontSize = 14.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

// ─── Loading Shimmer Skeleton ────────────────────────────────

@Composable
private fun LoadingShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header shimmer: greeting line
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .alpha(shimmerAlpha)
                .background(DarkCard)
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Date line
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(6.dp))
                .alpha(shimmerAlpha)
                .background(DarkCard)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Hero balance card shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(24.dp))
                .alpha(shimmerAlpha)
                .background(DarkCard)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Quick actions row shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(4) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .alpha(shimmerAlpha)
                            .background(DarkCard)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .alpha(shimmerAlpha)
                            .background(DarkCard)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Section title shimmer
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .alpha(shimmerAlpha)
                    .background(DarkCard)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .alpha(shimmerAlpha)
                    .background(DarkCard)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Account card skeletons
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .alpha(shimmerAlpha)
                    .background(DarkCard)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─── Utility Functions ─────────────────────────────────────

private fun extractNameFromEmail(email: String): String {
    val localPart = email.substringBefore("@")
    val parts = localPart.split(".")
    return parts.joinToString(" ") { part ->
        part.replaceFirstChar { it.uppercaseChar() }
    }
}

private fun formatCurrency(amount: Double, currency: String): String {
    val formatter = NumberFormat.getNumberInstance(Locale("sr", "RS"))
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return "${formatter.format(amount)} $currency"
}

private fun accountCountLabel(count: Int): String {
    return when {
        count == 1 -> "racun"
        count in 2..4 -> "racuna"
        else -> "racuna"
    }
}

private fun formattedDate(): String {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy.", Locale("sr", "RS"))
    val formatted = today.format(formatter)
    return formatted.replaceFirstChar { it.uppercaseChar() }
}

private fun formatAccountNumber(number: String): String {
    // Format as XXX-XXXXXXXXXX-XX if it's 18 digits
    if (number.length == 18 && number.all { it.isDigit() }) {
        return "${number.substring(0, 3)}-${number.substring(3, 13)}-${number.substring(13)}"
    }
    return number
}

private fun currencySymbol(currency: String): String {
    return when (currency.uppercase()) {
        "RSD" -> "din"
        "EUR" -> "\u20AC"
        "USD" -> "$"
        "CHF" -> "Fr"
        "GBP" -> "\u00A3"
        "JPY" -> "\u00A5"
        "CAD" -> "C$"
        "AUD" -> "A$"
        else -> currency.take(2)
    }
}

private fun currencyGradient(currency: String): List<Color> {
    return when (currency.uppercase()) {
        "RSD" -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))       // blue
        "EUR" -> listOf(Indigo500, Violet600)                        // indigo-violet
        "USD" -> listOf(Color(0xFF22C55E), Color(0xFF16A34A))       // green
        "CHF" -> listOf(Color(0xFFEF4444), Color(0xFFDC2626))       // red
        "GBP" -> listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))       // purple
        "JPY" -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))       // amber
        "CAD" -> listOf(Color(0xFFEC4899), Color(0xFFDB2777))       // pink
        "AUD" -> listOf(Color(0xFF14B8A6), Color(0xFF0D9488))       // teal
        else -> listOf(Color(0xFF6B7280), Color(0xFF4B5563))        // gray
    }
}

private fun accountTypeBadge(accountType: String?): String {
    return when (accountType?.uppercase()) {
        "CURRENT", "TEKUCI" -> "Tekuci"
        "SAVINGS", "STEDNI" -> "Stedni"
        "BUSINESS", "POSLOVNI" -> "Poslovni"
        "FOREIGN_CURRENCY", "DEVIZNI" -> "Devizni"
        "PENSION", "PENZIONI" -> "Penzioni"
        else -> ""
    }
}

private fun statusColor(status: String?): Color {
    return when (status?.uppercase()) {
        "ACTIVE", "AKTIVNA" -> SuccessGreen
        "PENDING", "NA_CEKANJU" -> WarningYellow
        "BLOCKED", "BLOKIRAN", "FROZEN", "ZAMRZNUT" -> ErrorRed
        else -> TextMuted
    }
}

private fun statusLabel(status: String?): String {
    return when (status?.uppercase()) {
        "ACTIVE", "AKTIVNA" -> "Aktivan"
        "PENDING", "NA_CEKANJU" -> "Na cekanju"
        "BLOCKED", "BLOKIRAN" -> "Blokiran"
        "FROZEN", "ZAMRZNUT" -> "Zamrznut"
        else -> status ?: "Nepoznat"
    }
}
