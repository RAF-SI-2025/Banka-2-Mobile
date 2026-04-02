package com.example.banka_2_mobile.ui.portfolio

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.banka_2_mobile.data.model.PortfolioItem
import com.example.banka_2_mobile.data.model.PortfolioSummary
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.DarkCardBorder
import com.example.banka_2_mobile.ui.theme.ErrorRed
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.SuccessGreen
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import com.example.banka_2_mobile.ui.theme.WarningYellow
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════════════
// TODO: PortfolioScreen — User's securities portfolio
// ══════════════════════════════════════════════════════════════════════════════
//
// OVERVIEW:
//   Shows the logged-in user's stock/futures holdings with total value,
//   profit/loss summary, and individual position details.
//
// ──────────────────────────────────────────────────────────────────────────────
// UI LAYOUT (top to bottom, LazyColumn):
// ──────────────────────────────────────────────────────────────────────────────
//
//   1. PAGE HEADER (same pattern as HomeScreen)
//      - Row with gradient accent bar (w=4.dp, h=20.dp, Indigo500->Violet600)
//      - Title: "Portfolio" (17.sp, SemiBold, White)
//
//   2. SUMMARY CARD (gradient card, same style as HomeScreen TotalBalanceCard)
//      - Background: Brush.linearGradient(Indigo500, Violet600)
//      - RoundedCornerShape(20.dp), padding 24.dp
//      - Content:
//        * Label: "Ukupna vrednost" (14.sp, White.copy(alpha=0.7f))
//        * Total value: summary.totalValue formatted as currency
//          (32.sp, Bold, White, FontFamily.Default — same as TotalBalanceCard)
//        * Profit row:
//          - Label: "Profit/Gubitak:" (13.sp, White.copy(alpha=0.6f))
//          - Value: summary.totalProfit formatted with +/- sign
//            * Color: SuccessGreen for positive, ErrorRed for negative
//            * Also show summary.totalProfitPercent as "(+X.XX%)"
//        * Tax info (if taxOwed != null):
//          - "Porez na kapitalnu dobit: X.XXX RSD" (12.sp, White.copy(alpha=0.5f))
//
//   3. SECTION TITLE
//      - "Vase hartije" with gradient accent bar (same pattern as HomeScreen "Vasi racuni")
//
//   4. HOLDINGS LIST (LazyColumn items)
//      Each PortfolioItem rendered as a DarkCard:
//
//      ┌──────────────────────────────────────────────────┐
//      │  AAPL               Apple Inc.                   │
//      │  Kolicina: 50       Prosecna cena: 150.00       │
//      │  Trenutna: 182.63   P/L: +1,631.50 (+10.84%)   │
//      │                                      [Prodaj]   │
//      └──────────────────────────────────────────────────┘
//
//      - Container: DarkCard, RoundedCornerShape(16.dp), padding 20.dp
//      - Top row:
//        * Ticker: 16.sp, Bold, FontFamily.Monospace, Color.White
//        * Name: 13.sp, TextMuted (right side or below ticker)
//        * Type badge: pill (listingType), tiny, bg = DarkCardBorder
//      - Middle rows:
//        * "Kolicina:" label (12.sp, TextMuted) + quantity (14.sp, White)
//        * "Prosecna cena:" label + averageBuyPrice (Monospace, White)
//        * "Trenutna cena:" label + currentPrice (Monospace, White)
//        * If inOrderQuantity > 0: show "U nalozima: X" (12.sp, WarningYellow)
//      - Profit/Loss row:
//        * profit formatted with +/- and 2 decimal places
//        * profitPercent formatted as "(+X.XX%)" or "(-X.XX%)"
//        * Color: SuccessGreen if profit >= 0, ErrorRed if < 0
//      - "Prodaj" button (small, right-aligned):
//        * bg = Brush.linearGradient(ErrorRed, Color(0xFFDC2626))
//        * text = "Prodaj" (White, 12.sp, SemiBold)
//        * RoundedCornerShape(8.dp), small padding
//        * onClick: onSellClick(item) -> navigate to CreateOrderScreen
//          with listingId from this item's data, direction = "SELL"
//      - Spacing between items: 12.dp
//
//   5. EMPTY STATE (when holdings list is empty)
//      - CircleShape icon container (64.dp, bg = DarkCard)
//        * Emoji: briefcase or chart emoji
//      - Title: "Nemate hartija u portfoliu" (17.sp, Medium, White)
//      - Subtitle: "Kupite prvu hartiju na berzi" (13.sp, TextMuted)
//      - Optional: "Idi na berzu" button (gradient, navigates to SecuritiesScreen)
//
//   6. BOTTOM SPACING
//      - Spacer(height = 90.dp) for BottomNavBar clearance
//
// ──────────────────────────────────────────────────────────────────────────────
// STATE VARIABLES:
// ──────────────────────────────────────────────────────────────────────────────
//
//   var holdings by remember { mutableStateOf<List<PortfolioItem>>(emptyList()) }
//   var summary by remember { mutableStateOf<PortfolioSummary?>(null) }
//   var isLoading by remember { mutableStateOf(true) }
//   var isRefreshing by remember { mutableStateOf(false) }
//   var errorMessage by remember { mutableStateOf<String?>(null) }
//
// ──────────────────────────────────────────────────────────────────────────────
// API CALLS:
// ──────────────────────────────────────────────────────────────────────────────
//
//   suspend fun fetchPortfolio() {
//       try {
//           // Fetch both in parallel or sequentially:
//           val holdingsResponse = RetrofitClient.api.getMyPortfolio()
//           val summaryResponse = RetrofitClient.api.getPortfolioSummary()
//
//           if (holdingsResponse.isSuccessful) {
//               holdings = holdingsResponse.body() ?: emptyList()
//           }
//           if (summaryResponse.isSuccessful) {
//               summary = summaryResponse.body()
//           }
//
//           if (holdingsResponse.code() == 401 || summaryResponse.code() == 401) {
//               authRepository.clearTokens()
//               onLogout()
//               return
//           }
//       } catch (e: Exception) {
//           errorMessage = "Greska u mrezi. Proverite konekciju."
//       }
//       isLoading = false
//       isRefreshing = false
//   }
//
//   LaunchedEffect(Unit) { fetchPortfolio() }
//   Pull-to-refresh: { isRefreshing = true; fetchPortfolio() }
//
// ──────────────────────────────────────────────────────────────────────────────
// NAVIGATION:
// ──────────────────────────────────────────────────────────────────────────────
//
//   Parameters:
//     onLogout: () -> Unit
//     onSellClick: (Long, String) -> Unit  — (listingId, "SELL") -> CreateOrderScreen
//
//   TODO: PortfolioItem may not contain listingId directly — might need to
//         look up the listing by ticker. Confirm with backend DTO.
//
// ──────────────────────────────────────────────────────────────────────────────
// DESIGN TOKENS:
// ──────────────────────────────────────────────────────────────────────────────
//
//   Same as SecuritiesScreen. Refer to Color.kt constants.
//   Summary card uses same gradient pattern as HomeScreen TotalBalanceCard.
//   Profit green: SuccessGreen (#22C55E)
//   Loss red: ErrorRed (#EF4444)
//   Locked-in-orders: WarningYellow (#EAB308)
//
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onLogout: () -> Unit,
    onSellClick: (Long, String) -> Unit  // (listingId, direction)
) {
    // TODO: Implement the full screen following the layout described above.
    //       Use HomeScreen.kt as the primary reference for:
    //       - TotalBalanceCard gradient pattern -> reuse for PortfolioSummary card
    //       - LazyColumn with items() pattern
    //       - EmptyAccountsState pattern -> adapt for empty portfolio
    //       - LoadingShimmer pattern
    //       - PullToRefreshBox wrapping
    //       - SnackbarHost for errors

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch { fetchPortfolio() }
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // ── 1. PAGE HEADER ──────────────────────────────────────────
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
                            text = "Portfolio",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── 2. SUMMARY CARD ─────────────────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Indigo500, Violet600),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(
                                text = "Ukupna vrednost",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = summary?.let { formatCurrency(it.totalValue) } ?: "—",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Default
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            summary?.let { s ->
                                val profitColor = if (s.totalProfit >= 0) SuccessGreen else ErrorRed
                                val profitSign = if (s.totalProfit >= 0) "+" else ""

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Profit/Gubitak: ",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "$profitSign${formatCurrency(s.totalProfit)} (${profitSign}${
                                            formatPercent(
                                                s.totalProfitPercent!!
                                            )
                                        }%)",
                                        fontSize = 13.sp,
                                        color = profitColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                s.taxOwed?.let { tax ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Porez na kapitalnu dobit: ${formatCurrency(tax)} RSD",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── 3. SECTION TITLE ────────────────────────────────────────
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(16.dp)
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
                            text = "Vaše hartije",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ── 4. HOLDINGS LIST ────────────────────────────────────────
                if (holdings.isNotEmpty()) {
                    items(holdings) { item ->
                        PortfolioItemCard(
                            item = item,
                            onSellClick = { onSellClick(item.id, "SELL") }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // ── 5. EMPTY STATE ──────────────────────────────────────────
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
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(DarkCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "📊", fontSize = 28.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Nemate hartija u portfoliu",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Kupite prvu hartiju na berzi",
                                fontSize = 13.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // ── 6. BOTTOM SPACING ───────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(90.dp))
                }

                /*Text(
                    text = "Portfolio - TODO",
                    color = Color.White,
                    fontSize = 20.sp
                )*/
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}


// ── PortfolioItemCard ────────────────────────────────────────────────────────

@Composable
private fun PortfolioItemCard(
    item: PortfolioItem,
    onSellClick: () -> Unit
) {
    val profit = item.profit ?: 0.0
    val profitPercent = item.profitPercent ?: 0.0
    val profitColor = if (profit >= 0) SuccessGreen else ErrorRed
    val profitSign = if (profit >= 0) "+" else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .padding(20.dp)
    ) {
        Column {
            // Ticker + type badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.listingTicker,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                item.listingType?.let { type ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                DarkCardBorder)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = type,
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Name
            item.listingName?.let { name ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = name, fontSize = 13.sp, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Količina + Prosečna cena
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Količina", fontSize = 12.sp, color = TextMuted)
                    Text(
                        text = item.quantity.toString(),
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Prosečna cena", fontSize = 12.sp, color = TextMuted)
                    Text(
                        text = formatCurrency(item.averageBuyPrice),
                        fontSize = 14.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Trenutna cena + U nalozima
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Trenutna cena", fontSize = 12.sp, color = TextMuted)
                    Text(
                        text = formatCurrency(item.currentPrice!!),
                        fontSize = 14.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if ((item.inOrderQuantity ?: 0) > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "U nalozima", fontSize = 12.sp, color = TextMuted)
                        Text(
                            text = item.inOrderQuantity.toString(),
                            fontSize = 14.sp,
                            color = WarningYellow,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Profit/Loss + Prodaj dugme
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Profit/Gubitak", fontSize = 12.sp, color = TextMuted)
                    Text(
                        text = "$profitSign${formatCurrency(profit)} (${profitSign}${
                            formatPercent(
                                profitPercent
                            )
                        }%)",
                        fontSize = 14.sp,
                        color = profitColor,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ErrorRed, Color(0xFFDC2626)),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, 0f)
                            )
                        )
                ) {
                    Button(
                        onClick = onSellClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Text(
                            text = "Prodaj",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun formatCurrency(value: Double): String {
    return "%,.2f RSD".format(value)
}

private fun formatPercent(value: Double): String {
    return "%.2f".format(value)
}
