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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.example.banka_2_mobile.ui.theme.TextWhite
import com.example.banka_2_mobile.ui.theme.Violet600
import com.example.banka_2_mobile.ui.theme.WarningYellow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale


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
                // -- 1. HEADER -------------------------------------------------------
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
                            color = TextWhite
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // -- 3. LOADING STATE ------------------------------------------------
                if (isLoading) {
                    items(4) {
                        OrderShimmerCard()
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // -- 2. ORDERS LIST --------------------------------------------------
                if (!isLoading) {
                    if (orders.isNotEmpty()) {
                        items(orders) { order ->
                            OrderCard(order = order)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        // -- 4. EMPTY STATE ----------------------------------------------
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
                                    Text(text = "\uD83D\uDCCB", fontSize = 28.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Nemate naloge",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextWhite,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Kada kreirate nalog na berzi, pojavi\u0107e se ovde",
                                    fontSize = 13.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // -- 6. BOTTOM SPACING -----------------------------------------------
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

    val sColor = statusColor(order.status)
    val statusLabel = statusLabel(order.status)

    val filled = order.filledQuantity
    val isPartial = filled != null && filled > 0 && filled != order.quantity

    // Card with colored left border matching status
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
    ) {
        // Status-colored left accent border
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(
                    Brush.linearGradient(
                        colors = listOf(sColor, sColor.copy(alpha = 0.5f)),
                        start = Offset(0f, 0f),
                        end = Offset(0f, Float.POSITIVE_INFINITY)
                    )
                )
        )

        // Card content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
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
                        color = TextWhite
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
                        .background(sColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = sColor
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity
                Row {
                    Text(
                        text = "Koli\u010Dina: ",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "${order.quantity}",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = TextWhite
                    )
                    if (filled != null && filled != order.quantity) {
                        Text(
                            text = " (izvr\u0161eno: $filled)",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }

                // AON badge
                if (order.allOrNone == true) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkCardBorder)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "AON",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                    }
                }
            }

            // Progress bar for partially executed orders
            if (isPartial) {
                Spacer(modifier = Modifier.height(8.dp))
                val progress = filled!!.toFloat() / order.quantity.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(DarkCardBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Indigo500, Violet600)
                                )
                            )
                    )
                }
                Text(
                    text = "${"%.0f".format(progress * 100)}% izvr\u0161eno",
                    fontSize = 11.sp,
                    color = Indigo400,
                    modifier = Modifier.padding(top = 4.dp)
                )
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
                color = TextWhite,
                fontFamily = FontFamily.Monospace
            )

            // Fee
            order.fee?.let { fee ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Provizija: ${"%.2f".format(fee)}",
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
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
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            DarkCard.copy(alpha = 0.3f),
            DarkCard.copy(alpha = 0.6f),
            DarkCard.copy(alpha = 0.3f)
        ),
        start = Offset(shimmerTranslate, 0f),
        end = Offset(shimmerTranslate + 300f, 0f)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard.copy(alpha = 0.4f))
    ) {
        // Shimmer left accent
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(DarkCardBorder)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            // Top row: ticker placeholder + badge placeholders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerBrush)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            // Order type placeholder
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(10.dp))
            // Details row placeholder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            // Date placeholder
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

// -- Helpers ------------------------------------------------------------------

private fun statusLabel(status: String): String = when (status.uppercase()) {
    "PENDING" -> "Na \u010Dekanju"
    "APPROVED" -> "Odobren"
    "DONE" -> "Izvr\u0161en"
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
