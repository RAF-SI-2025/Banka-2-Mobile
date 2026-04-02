package com.example.banka_2_mobile.ui.orders

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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.banka_2_mobile.data.model.OrderResponse
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
import com.example.banka_2_mobile.ui.theme.WarningYellow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════════════
// TODO: MyOrdersScreen — List of user's submitted orders
// ══════════════════════════════════════════════════════════════════════════════
//
// OVERVIEW:
//   Shows all orders placed by the current user with their status,
//   direction, quantity, and type information.
//
// ──────────────────────────────────────────────────────────────────────────────
// UI LAYOUT (top to bottom, LazyColumn with PullToRefreshBox):
// ──────────────────────────────────────────────────────────────────────────────
//
//   1. PAGE HEADER (same pattern as HomeScreen)
//      - Row with gradient accent bar (w=4.dp, h=20.dp, Indigo500->Violet600)
//      - Title: "Moji nalozi" (17.sp, SemiBold, White)
//
//   2. ORDERS LIST (LazyColumn items)
//      Each OrderResponse rendered as a DarkCard:
//
//      ┌──────────────────────────────────────────────────┐
//      │  AAPL  [KUPI]                    [NA CEKANJU]   │
//      │  Market nalog                                    │
//      │  Kolicina: 50    Cena: Market                   │
//      │  Datum: 27.03.2026                               │
//      └──────────────────────────────────────────────────┘
//
//      - Container: DarkCard, RoundedCornerShape(16.dp), padding 16.dp
//      - Top row (SpaceBetween):
//        * Left side:
//          - Ticker: 16.sp, Bold, FontFamily.Monospace, Color.White
//            (order.listingTicker ?: "N/A")
//          - Direction badge (pill):
//            * BUY: bg = SuccessGreen.copy(alpha=0.15f), text = SuccessGreen
//              label = "KUPI"
//            * SELL: bg = ErrorRed.copy(alpha=0.15f), text = ErrorRed
//              label = "PRODAJ"
//            * RoundedCornerShape(8.dp), padding h=10.dp v=4.dp
//            * 11.sp, SemiBold
//        * Right side:
//          - Status badge (pill):
//            * PENDING: bg = WarningYellow.copy(alpha=0.15f), text = WarningYellow
//              label = "Na cekanju"
//            * APPROVED: bg = Indigo400.copy(alpha=0.15f), text = Indigo400
//              label = "Odobren"
//            * DONE: bg = SuccessGreen.copy(alpha=0.15f), text = SuccessGreen
//              label = "Izvrsen"
//            * DECLINED: bg = ErrorRed.copy(alpha=0.15f), text = ErrorRed
//              label = "Odbijen"
//            * CANCELLED: bg = TextMuted.copy(alpha=0.15f), text = TextMuted
//              label = "Otkazan"
//            * RoundedCornerShape(8.dp), padding h=10.dp v=4.dp
//            * 11.sp, Medium
//
//      - Order type line:
//        * Map orderType to display: "MARKET"->"Market nalog", "LIMIT"->"Limit nalog",
//          "STOP"->"Stop nalog", "STOP_LIMIT"->"Stop-Limit nalog"
//        * 13.sp, TextMuted
//
//      - Details row:
//        * "Kolicina: X" (13.sp, White)
//          - If filledQuantity != null and filledQuantity != quantity:
//            append " (izvrseno: Y)" in TextMuted
//        * Price info (13.sp, Monospace, White):
//          - MARKET: "Cena: Market"
//          - LIMIT: "Limit: X.XX"
//          - STOP: "Stop: X.XX"
//          - STOP_LIMIT: "Limit: X.XX / Stop: Y.YY"
//        * Fee (if not null): "Provizija: X.XX" (12.sp, TextMuted)
//
//      - Date row:
//        * "Datum:" + formatted createdAt (13.sp, TextMuted)
//        * Format: parse ISO date, display as "dd.MM.yyyy HH:mm"
//
//      - If allOrNone == true: show small "AON" pill badge (DarkCardBorder bg, TextMuted text)
//
//      - Spacing between items: 12.dp
//
//   3. LOADING STATE
//      - 4 shimmer skeleton cards (fillMaxWidth, h=100.dp, RoundedCornerShape(16.dp))
//      - Same pattern as HomeScreen LoadingShimmer
//
//   4. EMPTY STATE
//      - CircleShape icon container (64.dp, bg = DarkCard)
//        * Emoji or icon representing orders/receipts
//      - Title: "Nemate naloge" (17.sp, Medium, White)
//      - Subtitle: "Kada kreirate nalog na berzi, pojavice se ovde" (13.sp, TextMuted)
//
//   5. PULL-TO-REFRESH
//      - Wrap in PullToRefreshBox (same pattern as HomeScreen)
//
//   6. BOTTOM SPACING
//      - Spacer(height = 90.dp) for BottomNavBar clearance
// ──────────────────────────────────────────────────────────────────────────────
// NAVIGATION:
// ──────────────────────────────────────────────────────────────────────────────
//
//   Parameters:
//     onLogout: () -> Unit
//
// ──────────────────────────────────────────────────────────────────────────────



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    var orders by remember { mutableStateOf<List<OrderResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun fetchOrders() {
        try {
            val response = RetrofitClient.api.getMyOrders()
            if (response.isSuccessful) {
                orders = response.body() ?: emptyList()
            } else if (response.code() == 401) {
                authRepository.clearTokens()
                onLogout()
                return
            } else {
                errorMessage = "Greška pri učitavanju naloga (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Greška u mreži. Proverite konekciju."
        }
        isLoading = false
        isRefreshing = false
    }

    LaunchedEffect(Unit) { fetchOrders() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackBarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch { fetchOrders() }
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // ── 1. HEADER ───────────────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(20.dp)
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
                            text = "Moji nalozi",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── 3. LOADING STATE ────────────────────────────────────────
                if (isLoading) {
                    items(4) {
                        OrderShimmerCard()
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // ── 2. ORDERS LIST ──────────────────────────────────────────
                if (!isLoading) {
                    if (orders.isNotEmpty()) {
                        items(orders) { order ->
                            OrderCard(order = order)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        // ── 4. EMPTY STATE ──────────────────────────────────
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 60.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(DarkCard),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "📋", fontSize = 28.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Nemate naloge",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Kada kreirate nalog na berzi, pojaviće se ovde",
                                    fontSize = 13.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // ── 6. BOTTOM SPACING ───────────────────────────────────────
                item { Spacer(modifier = Modifier.height(90.dp)) }
            }
        }

        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun OrderCard(order: OrderResponse) {
    val isBuy = order.direction.uppercase() == "BUY"
    val directionColor = if (isBuy) SuccessGreen else ErrorRed
    val directionLabel = if (isBuy) "KUPI" else "PRODAJ"

    val statusColor = statusColor(order.status)
    val statusLabel = statusLabel(order.status)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .padding(16.dp)
    ) {
        Column {
            // Top row: ticker + direction badge | status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = order.listingTicker ?: "N/A",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    // Direction badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(directionColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = directionLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = directionColor
                        )
                    }
                }
                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Order type
            Text(
                text = orderTypeLabel(order.orderType),
                fontSize = 13.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Quantity
                val quantityText = buildString {
                    append("Količina: ${order.quantity}")
                    val filled = order.filledQuantity
                    if (filled != null && filled != order.quantity) {
                        append(" (izvršeno: $filled)")
                    }
                }
                Text(text = quantityText, fontSize = 13.sp, color = Color.White)

                // AON badge
                if (order.allOrNone == true) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkCardBorder)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(text = "AON", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Price info
            val priceText = when (order.orderType.uppercase()) {
                "MARKET" -> "Cena: Market"
                "LIMIT" -> "Limit: ${"%.2f".format(order.limitPrice ?: 0.0)}"
                "STOP" -> "Stop: ${"%.2f".format(order.stopPrice ?: 0.0)}"
                "STOP_LIMIT" -> "Limit: ${"%.2f".format(order.limitPrice ?: 0.0)} / Stop: ${
                    "%.2f".format(
                        order.stopPrice ?: 0.0
                    )
                }"

                else -> "-"
            }
            Text(
                text = priceText,
                fontSize = 13.sp,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )

            // Fee
            order.fee?.let { fee ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Provizija: ${"%.2f".format(fee)}",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Date
            Text(
                text = "Datum: ${formatOrderDate(order.createdAt)}",
                fontSize = 13.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun OrderShimmerCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shimmer"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard.copy(alpha = alpha))
    )
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun statusLabel(status: String): String = when (status.uppercase()) {
    "PENDING" -> "Na čekanju"
    "APPROVED" -> "Odobren"
    "DONE" -> "Izvršen"
    "DECLINED" -> "Odbijen"
    "CANCELLED" -> "Otkazan"
    else -> status
}

private fun statusColor(status: String): Color = when (status.uppercase()) {
    "PENDING" -> WarningYellow
    "APPROVED" -> Indigo400
    "DONE" -> SuccessGreen
    "DECLINED" -> ErrorRed
    "CANCELLED" -> TextMuted
    else -> TextMuted
}

private fun orderTypeLabel(type: String): String = when (type.uppercase()) {
    "MARKET" -> "Market nalog"
    "LIMIT" -> "Limit nalog"
    "STOP" -> "Stop nalog"
    "STOP_LIMIT" -> "Stop-Limit nalog"
    else -> type
}

private fun formatOrderDate(isoDate: String?): String {
    if (isoDate == null) return "-"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(isoDate)
        if (date != null) outputFormat.format(date) else isoDate
    } catch (e: Exception) {
        isoDate
    }
}
