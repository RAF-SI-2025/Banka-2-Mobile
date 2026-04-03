package com.example.banka_2_mobile.ui.securities

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.model.Listing
import com.example.banka_2_mobile.data.model.ListingDailyPrice
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.DarkCardBorder
import com.example.banka_2_mobile.ui.theme.ErrorRed
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.SuccessGreen
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import java.time.LocalTime
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDetailScreen(
    listingId: Long,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onOrderClick: (Long, String) -> Unit  // (listingId, direction)
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var listing by remember { mutableStateOf<Listing?>(null) }
    var history by remember { mutableStateOf<List<ListingDailyPrice>>(emptyList()) }
    var selectedPeriod by remember { mutableStateOf("MONTH") }
    var isLoading by remember { mutableStateOf(true) }
    var isChartLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Capture the time when listing loads for "last updated"
    var lastUpdatedTime by remember { mutableStateOf("") }

    LaunchedEffect(listingId) {
        try {
            val response = RetrofitClient.api.getListingById(listingId)
            if (response.isSuccessful) {
                listing = response.body()
                lastUpdatedTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            } else if (response.code() == 401) {
                authRepository.clearTokens()
                onLogout()
                return@LaunchedEffect
            } else {
                errorMessage = "Greška pri učitavanju (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Greška u mreži."
        }
        isLoading = false
    }

    LaunchedEffect(listingId, selectedPeriod) {
        isChartLoading = true
        try {
            val response = RetrofitClient.api.getListingHistory(listingId, selectedPeriod)
            if (response.isSuccessful) {
                history = response.body() ?: emptyList()
            }
        } catch (e: Exception) {
            history = emptyList()
        }
        isChartLoading = false
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    val periods = listOf("1D" to "DAY", "1N" to "WEEK", "1M" to "MONTH", "1G" to "YEAR")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)  // clearance for bottom nav
        ) {
            // ── 1. TOP BAR ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Nazad",
                        tint = Color.White
                    )
                }

                if (!isLoading && listing != null) {
                    listing?.let { l ->
                        Text(
                            text = l.ticker,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        PremiumTypeBadge(text = l.listingType)
                        l.exchangeAcronym?.let { exch ->
                            Spacer(modifier = Modifier.width(6.dp))
                            PremiumExchangeBadge(text = exch)
                        }
                    }
                }
            }

            if (isLoading) {
                DetailShimmer()
            } else {
                listing?.let { l ->
                    // ── 2. PRICE HERO SECTION ──────────────────────────────
                    val changePercent = l.changePercent ?: 0.0
                    val isPositive = changePercent >= 0
                    val priceColor = if (isPositive) SuccessGreen else ErrorRed
                    val sign = if (isPositive) "+" else ""
                    val arrowIcon = if (isPositive)
                        Icons.Filled.KeyboardArrowUp
                    else
                        Icons.Filled.KeyboardArrowDown

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 8.dp)
                    ) {
                        // Security name
                        Text(
                            text = l.name,
                            fontSize = 14.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Big price
                        Text(
                            text = "%.2f".format(l.price),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Change row: arrow + absolute change + percentage pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = arrowIcon,
                                contentDescription = null,
                                tint = priceColor,
                                modifier = Modifier.size(20.dp)
                            )

                            l.priceChange?.let {
                                Text(
                                    text = "${sign}${"%.2f".format(it)}",
                                    fontSize = 15.sp,
                                    color = priceColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Percentage pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(priceColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${sign}${"%.2f".format(changePercent)}%",
                                    fontSize = 13.sp,
                                    color = priceColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Last updated
                        if (lastUpdatedTime.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Poslednje ažuriranje: $lastUpdatedTime",
                                fontSize = 11.sp,
                                color = TextMuted.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── 3. CHART SECTION ────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        // Period selector pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkCard)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            periods.forEach { (label, period) ->
                                val isSelected = selectedPeriod == period
                                val bgColor by animateColorAsState(
                                    targetValue = if (isSelected) Indigo500 else Color.Transparent,
                                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                                    label = "periodBg"
                                )
                                val textColor by animateColorAsState(
                                    targetValue = if (isSelected) Color.White else TextMuted,
                                    animationSpec = tween(250),
                                    label = "periodText"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bgColor)
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { selectedPeriod = period }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Chart area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkCard)
                                .border(
                                    width = 1.dp,
                                    color = DarkCardBorder.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                        ) {
                            if (isChartLoading) {
                                CircularProgressIndicator(
                                    color = Indigo500,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .align(Alignment.Center)
                                )
                            } else if (history.isEmpty()) {
                                Text(
                                    text = "Nema podataka",
                                    color = TextMuted,
                                    fontSize = 14.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else {
                                PremiumPriceChart(history = history)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── 4. STATS GRID ────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Section header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Indigo500, Violet600),
                                            start = Offset(0f, 0f),
                                            end = Offset(0f, Float.POSITIVE_INFINITY)
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Detalji",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        // Stats rows
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PremiumStatCard(
                                label = "ASK",
                                value = l.ask?.let { "%.2f".format(it) } ?: "-",
                                modifier = Modifier.weight(1f)
                            )
                            PremiumStatCard(
                                label = "BID",
                                value = l.bid?.let { "%.2f".format(it) } ?: "-",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PremiumStatCard(
                                label = "DNEVNI MAX",
                                value = l.high?.let { "%.2f".format(it) } ?: "-",
                                modifier = Modifier.weight(1f),
                                valueColor = SuccessGreen.copy(alpha = 0.9f)
                            )
                            PremiumStatCard(
                                label = "DNEVNI MIN",
                                value = l.low?.let { "%.2f".format(it) } ?: "-",
                                modifier = Modifier.weight(1f),
                                valueColor = ErrorRed.copy(alpha = 0.9f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PremiumStatCard(
                                label = "OBIM",
                                value = formatVolume(l.volume),
                                modifier = Modifier.weight(1f)
                            )
                            if (l.listingType == "STOCK") {
                                PremiumStatCard(
                                    label = "TRŽIŠNA KAP.",
                                    value = formatLargeNumber(l.marketCap),
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                PremiumStatCard(
                                    label = "VEL. UGOVORA",
                                    value = l.contractSize?.toString() ?: "-",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (l.listingType == "FUTURES") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PremiumStatCard(
                                    label = "MARGINA ODRŽ.",
                                    value = l.maintenanceMargin?.let { "%.2f".format(it) } ?: "-",
                                    modifier = Modifier.weight(1f)
                                )
                                PremiumStatCard(
                                    label = "DATUM ISTEKA",
                                    value = l.settlementDate ?: "-",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PremiumStatCard(
                                    label = "DIV. PRINOS",
                                    value = l.dividendYield?.let { "${"%.2f".format(it)}%" } ?: "-",
                                    modifier = Modifier.weight(1f)
                                )
                                PremiumStatCard(
                                    label = "BR. AKCIJA",
                                    value = formatVolume(l.outstandingShares),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // ── 5. ACTION BUTTONS ────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // BUY button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .shadow(
                                    elevation = 12.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = SuccessGreen.copy(alpha = 0.3f),
                                    spotColor = SuccessGreen.copy(alpha = 0.4f)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF10B981), // emerald-500
                                            Color(0xFF059669)  // emerald-600
                                        )
                                    )
                                )
                        ) {
                            Button(
                                onClick = { onOrderClick(l.id, "BUY") },
                                modifier = Modifier.fillMaxSize(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Kupi",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        // SELL button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .shadow(
                                    elevation = 12.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = ErrorRed.copy(alpha = 0.3f),
                                    spotColor = ErrorRed.copy(alpha = 0.4f)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFF43F5E), // rose-500
                                            Color(0xFFE11D48)  // rose-600
                                        )
                                    )
                                )
                        ) {
                            Button(
                                onClick = { onOrderClick(l.id, "SELL") },
                                modifier = Modifier.fillMaxSize(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Prodaj",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
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


// ═════════════════════════════════════════════════════════════════════════════
// Premium Chart with gradient fill and grid lines
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PremiumPriceChart(history: List<ListingDailyPrice>) {
    val prices = history.map { it.price.toFloat() }
    val lineColor = if (prices.last() >= prices.first()) SuccessGreen else ErrorRed

    Canvas(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (prices.size < 2) return@Canvas

        val minPrice = prices.min()
        val maxPrice = prices.max()
        val priceRange = (maxPrice - minPrice).coerceAtLeast(0.01f)
        val paddingTop = 16f
        val paddingBottom = 8f
        val chartHeight = size.height - paddingTop - paddingBottom
        val stepX = size.width / (prices.size - 1).coerceAtLeast(1)

        // Draw subtle horizontal grid lines
        val gridLineCount = 4
        val gridColor = DarkCardBorder.copy(alpha = 0.35f)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        for (i in 0..gridLineCount) {
            val y = paddingTop + (chartHeight * i / gridLineCount)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.8f,
                pathEffect = dashEffect
            )
        }

        // Build the line path
        val path = Path()
        val fillPath = Path()

        prices.forEachIndexed { i, price ->
            val x = i * stepX
            val y = paddingTop + (1f - (price - minPrice) / priceRange) * chartHeight
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        // Close fill path along the bottom
        fillPath.lineTo(size.width, size.height)
        fillPath.lineTo(0f, size.height)
        fillPath.close()

        // Gradient fill under the line
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.25f),
                    lineColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = size.height
            )
        )

        // Main line with glow
        drawPath(
            path = path,
            color = lineColor.copy(alpha = 0.3f),
            style = Stroke(width = 6.dp.toPx())
        )
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )

        // End-point dot
        val lastX = (prices.size - 1) * stepX
        val lastY = paddingTop + (1f - (prices.last() - minPrice) / priceRange) * chartHeight
        drawCircle(
            color = lineColor,
            radius = 4.dp.toPx(),
            center = Offset(lastX, lastY)
        )
        drawCircle(
            color = lineColor.copy(alpha = 0.25f),
            radius = 8.dp.toPx(),
            center = Offset(lastX, lastY)
        )
    }
}


// ═════════════════════════════════════════════════════════════════════════════
// Premium Stat Card
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PremiumStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .border(
                width = 1.dp,
                color = DarkCardBorder.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted.copy(alpha = 0.7f),
                letterSpacing = 1.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


// ═════════════════════════════════════════════════════════════════════════════
// Premium Badges
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PremiumTypeBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Indigo500.copy(alpha = 0.20f),
                        Violet600.copy(alpha = 0.15f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Indigo400.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = Indigo400,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun PremiumExchangeBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DarkCardBorder.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = DarkCardBorder.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}


// ═════════════════════════════════════════════════════════════════════════════
// Shimmer loading placeholder
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun DetailShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shimmer"
    )
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Price block
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard.copy(alpha = alpha))
        )
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkCard.copy(alpha = alpha))
        )
        // Chart block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkCard.copy(alpha = alpha))
        )
        // Stats blocks
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkCard.copy(alpha = alpha))
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkCard.copy(alpha = alpha))
                )
            }
        }
    }
}


// ═════════════════════════════════════════════════════════════════════════════
// Formatters (unchanged)
// ═════════════════════════════════════════════════════════════════════════════

private fun formatVolume(volume: Long?): String {
    if (volume == null) return "-"
    return when {
        volume >= 1_000_000_000 -> "${"%.2f".format(volume / 1_000_000_000.0)}B"
        volume >= 1_000_000 -> "${"%.2f".format(volume / 1_000_000.0)}M"
        volume >= 1_000 -> "${"%.2f".format(volume / 1_000.0)}K"
        else -> volume.toString()
    }
}

private fun formatLargeNumber(value: Long?): String = formatVolume(value)
