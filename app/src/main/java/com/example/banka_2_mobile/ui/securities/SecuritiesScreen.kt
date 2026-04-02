package com.example.banka_2_mobile.ui.securities

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.model.Listing
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.DarkCardBorder
import com.example.banka_2_mobile.ui.theme.ErrorRed
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.SuccessGreen
import com.example.banka_2_mobile.ui.theme.ErrorRed
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import kotlinx.coroutines.delay
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

    ///////////////
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var activeTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }
    var currentPage by remember { mutableIntStateOf(0) }

    val listingType = if (activeTab == 0) "STOCK" else "FUTURES"
    val tabs = listOf("Akcije", "Futures")

    // Debounce search
    LaunchedEffect(searchQuery) {
        delay(300)
        debouncedQuery = searchQuery
    }

    suspend fun fetchListings() {
        try {
            val response = RetrofitClient.api.getListings(
                type = listingType,
                search = debouncedQuery,
                page = currentPage,
                size = 20
            )
            if (response.isSuccessful) {
                val body = response.body()
                listings = body?.content ?: emptyList()
                totalPages = body?.totalPages ?: 0
            } else if (response.code() == 401) {
                authRepository.clearTokens()
                onLogout()
                return
            } else {
                errorMessage = "Greška pri učitavanju berze (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Greška u mreži. Proverite konekciju."
        }
        isLoading = false
        isRefreshing = false
    }

    LaunchedEffect(activeTab, debouncedQuery) {
        isLoading = true
        fetchListings()
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    ///////////////

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        /*Text(
            text = "Berza - TODO",
            color = Color.White,
            fontSize = 20.sp
        )*/

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch { fetchListings() }
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
                            text = "Berza",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── 2. TAB ROW ──────────────────────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCard)
                    ) {
                        TabRow(
                            selectedTabIndex = activeTab,
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            indicator = { tabPositions ->
                                Box(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[activeTab])
                                        .height(2.dp)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Indigo500, Violet600)
                                            )
                                        )
                                )
                            },
                            divider = {}
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = activeTab == index,
                                    onClick = {
                                        activeTab = index
                                        searchQuery = ""
                                        currentPage = 0
                                    },
                                    text = {
                                        Text(
                                            text = title,
                                            fontSize = 14.sp,
                                            fontWeight = if (activeTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (activeTab == index) Color.White else TextMuted
                                        )
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── 3. SEARCH BAR ───────────────────────────────────────────
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Pretražite hartije od vrednosti...",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = TextMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Indigo500,
                            unfocusedBorderColor = DarkCardBorder,
                            cursorColor = Indigo500
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── 5. LOADING STATE ────────────────────────────────────────
                if (isLoading) {
                    items(4) {
                        ListingShimmerCard()
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // ── 4. LISTINGS LIST ────────────────────────────────────────
                if (!isLoading) {
                    if (listings.isNotEmpty()) {
                        items(listings) { listing ->
                            ListingCard(
                                listing = listing,
                                onClick = { onListingClick(listing.id) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        // ── 6. EMPTY STATE ──────────────────────────────────
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
                                    Text(text = "🔍", fontSize = 28.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Nema rezultata",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Pokušajte drugu pretragu",
                                    fontSize = 13.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // ── 9. BOTTOM SPACING ───────────────────────────────────────
                item { Spacer(modifier = Modifier.height(90.dp)) }

            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}


@Composable
private fun ListingCard(listing: Listing, onClick: () -> Unit) {
    val changePercent = listing.changePercent ?: 0.0
    val isPositive = changePercent >= 0
    val changeColor = if (isPositive) SuccessGreen else ErrorRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .clickable { onClick() }
    ) {
        // Left colored border
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(80.dp)
                .background(changeColor)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: ticker, name, badges
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listing.ticker,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Text(
                    text = listing.name,
                    fontSize = 13.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallBadge(text = listing.listingType)
                    listing.exchangeAcronym?.let { SmallBadge(text = it) }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: price, change, volume
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatPrice(listing.price),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Text(
                    text = formatChangePercent(changePercent),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = changeColor
                )
                Text(
                    text = "Vol: ${formatVolume(listing.volume)}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun SmallBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkCardBorder)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ListingShimmerCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shimmer"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard.copy(alpha = shimmerAlpha))
    )
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun formatVolume(volume: Long?): String {
    if (volume == null) return "-"
    return when {
        volume >= 1_000_000_000 -> "${"%.2f".format(volume / 1_000_000_000.0)}B"
        volume >= 1_000_000 -> "${"%.2f".format(volume / 1_000_000.0)}M"
        volume >= 1_000 -> "${"%.2f".format(volume / 1_000.0)}K"
        else -> volume.toString()
    }
}

private fun formatChangePercent(percent: Double?): String {
    if (percent == null) return "0.00%"
    return if (percent >= 0) "+${"%.2f".format(percent)}%" else "${"%.2f".format(percent)}%"
}

private fun formatPrice(price: Double): String = "%.2f".format(price)
