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
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.SuccessGreen
import com.example.banka_2_mobile.ui.theme.ErrorRed
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Portfolio - TODO",
            color = Color.White,
            fontSize = 20.sp
        )
    }
}
