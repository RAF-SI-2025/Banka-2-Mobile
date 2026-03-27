package com.example.banka_2_mobile.ui.securities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.model.Listing
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.DarkCardBorder
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.SuccessGreen
import com.example.banka_2_mobile.ui.theme.ErrorRed
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════════════
// TODO: SecuritiesScreen — Berza listing screen for Android
// ══════════════════════════════════════════════════════════════════════════════
//
// OVERVIEW:
//   Main securities listing screen (Celina 3). Shows stocks and futures
//   available on the exchange. Clients cannot see Forex (per spec).
//
// ──────────────────────────────────────────────────────────────────────────────
// UI LAYOUT (top to bottom):
// ──────────────────────────────────────────────────────────────────────────────
//
//   1. PAGE HEADER
//      - Row with gradient accent bar (w=4.dp, h=20.dp, Indigo500->Violet600)
//      - Title: "Berza" (17.sp, SemiBold, White) — same pattern as HomeScreen
//
//   2. TAB ROW (two tabs)
//      - Tab 0: "Akcije" (STOCK)
//      - Tab 1: "Futures" (FUTURES)
//      - Use TabRow with indicator = Indigo500 gradient underline
//      - Background: DarkCard, selected text: White, unselected: TextMuted
//      - On tab change: reset searchQuery, set activeTab, re-fetch listings
//
//   3. SEARCH BAR
//      - OutlinedTextField with Search icon (leadingIcon)
//      - Placeholder: "Pretrazite hartije od vrednosti..."
//      - Colors: containerColor = DarkCard, text = White, border = DarkCardBorder
//      - focused border = Indigo500
//      - Debounce search input by 300ms before triggering API call
//      - RoundedCornerShape(12.dp)
//
//   4. LISTINGS LIST (LazyColumn)
//      Each item is a Row inside a DarkCard rounded box (16.dp corners):
//
//      ┌─────────────────────────────────────────────────┐
//      │ ▌ AAPL          Apple Inc.           $182.63   │
//      │ ▌ STOCK         NASDAQ               +1.25%   │
//      │                                      Vol: 52M  │
//      └─────────────────────────────────────────────────┘
//
//      - Left colored border: 4.dp wide vertical bar
//        * SuccessGreen (#22C55E) if changePercent >= 0
//        * ErrorRed (#EF4444) if changePercent < 0
//      - Ticker: 16.sp, Bold, FontFamily.Monospace, Color.White
//      - Name: 13.sp, TextMuted, maxLines = 1, ellipsis
//      - Type badge: tiny pill (listingType text), bg = DarkCardBorder
//      - Exchange badge: tiny pill (exchangeAcronym), bg = DarkCardBorder
//      - Price: 18.sp, Bold, FontFamily.Monospace, Color.White, right-aligned
//      - Change: 13.sp, FontFamily.Monospace
//        * Green text + "+" prefix if positive, Red text if negative
//        * Format: "+2.35%" or "-1.12%"
//      - Volume: 11.sp, TextMuted, right-aligned
//        * Abbreviate: 1,234,567 -> "1.23M", 500,000 -> "500K"
//      - On click: navigate to SecurityDetailScreen with listing.id
//      - Spacing between items: 12.dp
//
//   5. LOADING STATE
//      - Show shimmer placeholders (3-4 skeleton cards), same pattern as
//        HomeScreen LoadingShimmer. Each card: fillMaxWidth, h=80.dp,
//        RoundedCornerShape(16.dp), bg = DarkCard
//
//   6. EMPTY STATE
//      - Icon: magnifying glass emoji or search icon in a CircleShape bg=DarkCard
//      - Title: "Nema rezultata" (17.sp, Medium, White)
//      - Subtitle: "Pokusajte drugu pretragu" (13.sp, TextMuted)
//      - Same layout pattern as HomeScreen EmptyAccountsState
//
//   7. ERROR STATE
//      - Show Snackbar via SnackbarHostState (same as HomeScreen pattern)
//      - On 401: clear tokens + onLogout()
//
//   8. PULL-TO-REFRESH
//      - Wrap content in PullToRefreshBox (same as HomeScreen)
//      - On refresh: re-fetch with current activeTab + searchQuery
//
//   9. BOTTOM SPACING
//      - Spacer(height = 90.dp) at end of LazyColumn for BottomNavBar clearance
//
// ──────────────────────────────────────────────────────────────────────────────
// STATE VARIABLES:
// ──────────────────────────────────────────────────────────────────────────────
//
//   var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
//   var activeTab by remember { mutableIntStateOf(0) }  // 0 = STOCK, 1 = FUTURES
//   var searchQuery by remember { mutableStateOf("") }
//   var isLoading by remember { mutableStateOf(true) }
//   var isRefreshing by remember { mutableStateOf(false) }
//   var errorMessage by remember { mutableStateOf<String?>(null) }
//   var totalPages by remember { mutableIntStateOf(0) }
//   var currentPage by remember { mutableIntStateOf(0) }
//
//   // Derive listing type from tab index:
//   val listingType = if (activeTab == 0) "STOCK" else "FUTURES"
//
// ──────────────────────────────────────────────────────────────────────────────
// API CALLS:
// ──────────────────────────────────────────────────────────────────────────────
//
//   suspend fun fetchListings() {
//       try {
//           val response = RetrofitClient.api.getListings(
//               type = listingType,
//               search = searchQuery,
//               page = currentPage,
//               size = 20
//           )
//           if (response.isSuccessful) {
//               val body = response.body()
//               listings = body?.content ?: emptyList()
//               totalPages = body?.totalPages ?: 0
//           } else if (response.code() == 401) {
//               authRepository.clearTokens()
//               onLogout()
//               return
//           } else {
//               errorMessage = "Greska pri ucitavanju berze (${response.code()})"
//           }
//       } catch (e: Exception) {
//           errorMessage = "Greska u mrezi. Proverite konekciju."
//       }
//       isLoading = false
//       isRefreshing = false
//   }
//
//   - LaunchedEffect(activeTab, searchQuery) triggers fetchListings()
//   - Pull-to-refresh also calls fetchListings()
//
// ──────────────────────────────────────────────────────────────────────────────
// NAVIGATION:
// ──────────────────────────────────────────────────────────────────────────────
//
//   - Receives: onLogout: () -> Unit, onListingClick: (Long) -> Unit
//   - On listing item click: onListingClick(listing.id) -> NavGraph navigates
//     to "securities/{id}" route
//
// ──────────────────────────────────────────────────────────────────────────────
// DESIGN TOKENS:
// ──────────────────────────────────────────────────────────────────────────────
//
//   - Background: DarkBg (#070B24)
//   - Card: DarkCard (#0D1240)
//   - Card border: DarkCardBorder (#1E2563)
//   - Primary accent: Indigo500 (#6366F1) -> Violet600 (#7C3AED)
//   - Positive: SuccessGreen (#22C55E)
//   - Negative: ErrorRed (#EF4444)
//   - Muted text: TextMuted (#94A3B8)
//   - Monospace font for: ticker, price, change percentage
//
// ──────────────────────────────────────────────────────────────────────────────
// HELPER FUNCTIONS NEEDED:
// ──────────────────────────────────────────────────────────────────────────────
//
//   private fun formatVolume(volume: Long?): String
//       // null -> "-"
//       // >= 1_000_000_000 -> "X.XXB"
//       // >= 1_000_000 -> "X.XXM"
//       // >= 1_000 -> "X.XXK"
//       // else -> volume.toString()
//
//   private fun formatChangePercent(percent: Double?): String
//       // null -> "0.00%"
//       // positive -> "+X.XX%"
//       // negative -> "-X.XX%" (minus is natural)
//
//   private fun formatPrice(price: Double): String
//       // Use NumberFormat with 2 decimal places, Locale("sr", "RS")
//       // Append no currency (raw price)
//
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritiesScreen(
    onLogout: () -> Unit,
    onListingClick: (Long) -> Unit
) {
    // TODO: Implement the full screen following the layout described above.
    //       Use HomeScreen.kt as the reference for:
    //       - Background orb animation pattern
    //       - PullToRefreshBox wrapping
    //       - LazyColumn with spacing
    //       - SnackbarHost for errors
    //       - LoadingShimmer composable
    //       - EmptyState composable
    //       - 401 handling with onLogout()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Berza - TODO",
            color = Color.White,
            fontSize = 20.sp
        )
    }
}
