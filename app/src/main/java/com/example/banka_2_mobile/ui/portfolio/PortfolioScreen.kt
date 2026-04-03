package com.example.banka_2_mobile.ui.portfolio

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.model.PortfolioItem
import com.example.banka_2_mobile.data.model.PortfolioSummary
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.DarkCardBorder
import com.example.banka_2_mobile.ui.theme.DarkCardElevated
import com.example.banka_2_mobile.ui.theme.ErrorRed
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.SuccessGreen
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import com.example.banka_2_mobile.ui.theme.WarningYellow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onLogout: () -> Unit,
    onSellClick: (Long, String) -> Unit  // (listingId, direction)
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var holdings by remember { mutableStateOf<List<PortfolioItem>>(emptyList()) }
    var summary by remember { mutableStateOf<PortfolioSummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun fetchPortfolio() {
        try {
            val holdingsResponse = RetrofitClient.api.getMyPortfolio()
            val summaryResponse = RetrofitClient.api.getPortfolioSummary()

            if (holdingsResponse.code() == 401 || summaryResponse.code() == 401) {
                authRepository.clearTokens()
                onLogout()
                return
            }

            if (holdingsResponse.isSuccessful) {
                holdings = holdingsResponse.body() ?: emptyList()
            }
            if (summaryResponse.isSuccessful) {
                summary = summaryResponse.body()
            }
        } catch (e: Exception) {
            errorMessage = "Greška u mreži. Proverite konekciju."
        }
        isLoading = false
        isRefreshing = false
    }

    LaunchedEffect(Unit) { fetchPortfolio() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    // Background orb animation (same as HomeScreen)
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
                scope.launch { fetchPortfolio() }
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
                    PortfolioLoadingShimmer()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                    ) {
                        // ── 1. PAGE HEADER ──────────────────────────────────────
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                                Icon(
                                    imageVector = Icons.Filled.Work,
                                    contentDescription = null,
                                    tint = Indigo500,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Portfolio",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // ── 2. SUMMARY HERO CARD ────────────────────────────────
                        item {
                            PortfolioSummaryCard(summary)
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // ── 3. SECTION TITLE ────────────────────────────────────
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(18.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Indigo500, Violet600)
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Vaše hartije",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                if (holdings.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Indigo500.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = holdings.size.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Indigo500
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // ── 4. HOLDINGS LIST ────────────────────────────────────
                        if (holdings.isNotEmpty()) {
                            items(holdings) { item ->
                                PortfolioItemCard(
                                    item = item,
                                    onSellClick = { onSellClick(item.id, "SELL") }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        // ── 5. EMPTY STATE ──────────────────────────────────────
                        if (holdings.isEmpty() && !isLoading) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 60.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(DarkCard),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ShowChart,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = "Nemate hartija u portfoliu",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Kupite prvu hartiju na berzi",
                                        fontSize = 14.sp,
                                        color = TextMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // ── 6. BOTTOM SPACING ───────────────────────────────────
                        item {
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}


// ── Summary Hero Card ───────────────────────────────────────────────────────

@Composable
private fun PortfolioSummaryCard(summary: PortfolioSummary?) {
    // Shimmer animation on the card
    val infiniteTransition = rememberInfiniteTransition(label = "heroShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "heroShimmerOffset"
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
                text = "Ukupna vrednost",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = summary?.let { formatCurrency(it.totalValue) } ?: "—",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(14.dp))

            summary?.let { s ->
                val profitColor = if (s.totalProfit >= 0) SuccessGreen else ErrorRed
                val profitSign = if (s.totalProfit >= 0) "+" else ""
                val arrowIcon = if (s.totalProfit >= 0)
                    Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = arrowIcon,
                        contentDescription = null,
                        tint = profitColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "$profitSign${formatCurrency(s.totalProfit)}",
                        fontSize = 14.sp,
                        color = profitColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${profitSign}${formatPercent(s.totalProfitPercent ?: 0.0)}%)",
                        fontSize = 13.sp,
                        color = profitColor.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                s.taxOwed?.let { tax ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Porez na kapitalnu dobit: ${formatCurrency(tax)} RSD",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}


// ── Portfolio Item Card ─────────────────────────────────────────────────────

@Composable
private fun PortfolioItemCard(
    item: PortfolioItem,
    onSellClick: () -> Unit
) {
    val profit = item.profit ?: 0.0
    val profitPercent = item.profitPercent ?: 0.0
    val profitColor = if (profit >= 0) SuccessGreen else ErrorRed
    val profitSign = if (profit >= 0) "+" else ""
    val arrowIcon = if (profit >= 0) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
    ) {
        Column {
            // Main content
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Top row: Ticker + Type badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Ticker icon circle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Indigo500.copy(alpha = 0.2f),
                                            Violet600.copy(alpha = 0.2f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.listingTicker.take(2),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Indigo500
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = item.listingTicker,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            item.listingName?.let { name ->
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    item.listingType?.let { type ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkCardBorder)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = type,
                                fontSize = 10.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Data grid: 2x2 layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Quantity
                    Column {
                        Text(
                            text = "Količina",
                            fontSize = 11.sp,
                            color = TextMuted,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.quantity.toString(),
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    // Average buy price
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Prosečna cena",
                            fontSize = 11.sp,
                            color = TextMuted,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatCurrency(item.averageBuyPrice),
                            fontSize = 15.sp,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Current price
                    Column {
                        Text(
                            text = "Trenutna cena",
                            fontSize = 11.sp,
                            color = TextMuted,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatCurrency(item.currentPrice ?: 0.0),
                            fontSize = 15.sp,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // In-order quantity
                    if ((item.inOrderQuantity ?: 0) > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "U nalozima",
                                fontSize = 11.sp,
                                color = TextMuted,
                                letterSpacing = 0.3.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.inOrderQuantity.toString(),
                                fontSize = 15.sp,
                                color = WarningYellow,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profit/Loss row + Sell button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profit section
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = arrowIcon,
                            contentDescription = null,
                            tint = profitColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Profit/Gubitak",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "$profitSign${formatCurrency(profit)}",
                                fontSize = 15.sp,
                                color = profitColor,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(profitColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${profitSign}${formatPercent(profitPercent)}%",
                                fontSize = 11.sp,
                                color = profitColor,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Sell button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFF43F5E),  // Rose500
                                        Color(0xFFE11D48)   // Rose600
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, 0f)
                                )
                            )
                    ) {
                        Button(
                            onClick = onSellClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 18.dp,
                                vertical = 6.dp
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Prodaj",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
            }

            // Bottom gradient accent line (same pattern as HomeScreen AccountCard)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                profitColor.copy(alpha = 0.4f),
                                profitColor.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}


// ── Loading Shimmer Skeleton ────────────────────────────────────────────────

@Composable
private fun PortfolioLoadingShimmer() {
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
        Spacer(modifier = Modifier.height(20.dp))

        // Header shimmer: accent bar + icon + title
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
                    .size(22.dp)
                    .clip(CircleShape)
                    .alpha(shimmerAlpha)
                    .background(DarkCard)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .alpha(shimmerAlpha)
                    .background(DarkCard)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero summary card shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .alpha(shimmerAlpha)
                .background(DarkCardElevated)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Section title shimmer
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
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

        Spacer(modifier = Modifier.height(14.dp))

        // Holding card skeletons
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .alpha(shimmerAlpha)
                    .background(DarkCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Ticker row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .alpha(shimmerAlpha)
                                .background(DarkCardBorder)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .alpha(shimmerAlpha)
                                    .background(DarkCardBorder)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .alpha(shimmerAlpha)
                                    .background(DarkCardBorder)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Data rows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .alpha(shimmerAlpha)
                                .background(DarkCardBorder)
                        )
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .alpha(shimmerAlpha)
                                .background(DarkCardBorder)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .alpha(shimmerAlpha)
                                .background(DarkCardBorder)
                        )
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .alpha(shimmerAlpha)
                                .background(DarkCardBorder)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}


// ── Helpers ─────────────────────────────────────────────────────────────────

private fun formatCurrency(value: Double): String {
    return "%,.2f RSD".format(value)
}

private fun formatPercent(value: Double): String {
    return "%.2f".format(value)
}
