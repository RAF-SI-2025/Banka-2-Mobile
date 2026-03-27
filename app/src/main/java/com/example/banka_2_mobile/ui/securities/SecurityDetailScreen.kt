package com.example.banka_2_mobile.ui.securities

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.model.Listing
import com.example.banka_2_mobile.data.model.ListingDailyPrice
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.DarkCardBorder
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.SuccessGreen
import com.example.banka_2_mobile.ui.theme.ErrorRed
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600

// ══════════════════════════════════════════════════════════════════════════════
// TODO: SecurityDetailScreen — Detail view for a single security listing
// ══════════════════════════════════════════════════════════════════════════════
//
// OVERVIEW:
//   Shows detailed info for a stock or futures listing, including a price
//   history chart, key stats, and BUY/SELL action buttons.
//
// ──────────────────────────────────────────────────────────────────────────────
// UI LAYOUT (top to bottom, scrollable Column):
// ──────────────────────────────────────────────────────────────────────────────
//
//   1. TOP BAR
//      - Back arrow IconButton (Icons.AutoMirrored.Filled.ArrowBack)
//        tint = TextMuted, onClick = onBack()
//      - No title in top bar (ticker is shown below)
//
//   2. HEADER SECTION
//      - Ticker: 28.sp, Bold, FontFamily.Monospace, Color.White
//      - Name: 14.sp, TextMuted, below ticker
//      - Row of badges:
//        * Type badge: pill with listingType ("STOCK" / "FUTURES")
//          bg = Indigo500.copy(alpha=0.15f), text = Indigo400, 11.sp
//        * Exchange badge: pill with exchangeAcronym (e.g., "NASDAQ")
//          bg = DarkCardBorder, text = TextMuted, 11.sp
//
//   3. PRICE DISPLAY
//      - Current price: 36.sp, Bold, FontFamily.Monospace, Color.White
//      - Change row: changePercent formatted as "+2.35%" or "-1.12%"
//        * Color: SuccessGreen if >= 0, ErrorRed if < 0
//        * Also show absolute priceChange next to it (14.sp, same color)
//      - Format price using NumberFormat, Locale("sr", "RS"), 2 decimal places
//
//   4. PRICE CHART (Canvas-based line chart)
//      - Container: DarkCard rounded box, fillMaxWidth, height = 200.dp
//      - Period selector row above chart:
//        * Buttons: "1D", "1N", "1M", "1G" (Dan, Nedelja, Mesec, Godina)
//        * Map to API periods: "DAY", "WEEK", "MONTH", "YEAR"
//        * Selected button: bg = Indigo500, text = White
//        * Unselected: bg = transparent, text = TextMuted
//        * Default selected: "1M" (MONTH)
//      - Chart drawing (Compose Canvas):
//        * Fetch data: RetrofitClient.api.getListingHistory(listingId, period)
//        * Create Path connecting price points (x = evenly spaced, y = scaled)
//        * Line color: SuccessGreen if last price >= first price, else ErrorRed
//        * Line width: 2.dp stroke
//        * Fill area below line with gradient (lineColor.copy(alpha=0.3f) -> transparent)
//        * No axis labels needed (keep it clean like a sparkline)
//        * If history is empty, show "Nema podataka" centered text
//      - Loading state for chart: show pulsing DarkCard placeholder
//
//   5. STATS GRID (2 columns, inside DarkCard box)
//      - Title: "Detalji" with gradient accent bar (same as HomeScreen pattern)
//      - Grid items (label: value pairs):
//        Row 1: "Ask" : listing.ask       | "Bid" : listing.bid
//        Row 2: "Dnevni max" : listing.high | "Dnevni min" : listing.low
//        Row 3: "Obim" : formatVolume(listing.volume) | depends on type:
//          * STOCK: "Trzisna kap." : formatMarketCap(listing.marketCap)
//          * FUTURES: "Velicina ugovora" : listing.contractSize
//        Row 4 (FUTURES only):
//          "Margina odrzavanja" : listing.maintenanceMargin
//          "Datum isteka" : listing.settlementDate
//        Row 4 (STOCK only):
//          "Dividendni prinos" : listing.dividendYield (formatted as "X.XX%")
//          "Broj akcija" : formatVolume(listing.outstandingShares)
//      - Label: 12.sp, TextMuted
//      - Value: 14.sp, SemiBold, Color.White, FontFamily.Monospace
//      - Each cell: padding 12.dp, fill half width
//
//   6. ACTION BUTTONS (fixed at bottom or at end of scroll)
//      - Row with two buttons, equal width, 12.dp gap:
//      - BUY button:
//        * bg = Brush.linearGradient(SuccessGreen, Color(0xFF16A34A))
//        * Text: "KUPI" (White, Bold, 16.sp)
//        * shadow = SuccessGreen.copy(alpha=0.2f)
//        * onClick: onBuyClick(listing.id) -> navigate to CreateOrderScreen
//          with direction = "BUY"
//      - SELL button:
//        * bg = Brush.linearGradient(ErrorRed, Color(0xFFDC2626))
//        * Text: "PRODAJ" (White, Bold, 16.sp)
//        * shadow = ErrorRed.copy(alpha=0.2f)
//        * onClick: onSellClick(listing.id) -> navigate to CreateOrderScreen
//          with direction = "SELL"
//      - Both buttons: RoundedCornerShape(12.dp), height = 52.dp
//
//   7. BOTTOM SPACING
//      - Spacer(height = 100.dp) for BottomNavBar clearance
//
// ──────────────────────────────────────────────────────────────────────────────
// STATE VARIABLES:
// ──────────────────────────────────────────────────────────────────────────────
//
//   var listing by remember { mutableStateOf<Listing?>(null) }
//   var history by remember { mutableStateOf<List<ListingDailyPrice>>(emptyList()) }
//   var selectedPeriod by remember { mutableStateOf("MONTH") }
//   var isLoading by remember { mutableStateOf(true) }
//   var isChartLoading by remember { mutableStateOf(true) }
//   var errorMessage by remember { mutableStateOf<String?>(null) }
//
// ──────────────────────────────────────────────────────────────────────────────
// API CALLS:
// ──────────────────────────────────────────────────────────────────────────────
//
//   // Fetch listing detail on first load:
//   LaunchedEffect(listingId) {
//       try {
//           val response = RetrofitClient.api.getListingById(listingId)
//           if (response.isSuccessful) {
//               listing = response.body()
//           } else if (response.code() == 401) {
//               onLogout()
//               return@LaunchedEffect
//           } else {
//               errorMessage = "Greska pri ucitavanju (${response.code()})"
//           }
//       } catch (e: Exception) {
//           errorMessage = "Greska u mrezi."
//       }
//       isLoading = false
//   }
//
//   // Fetch history when period changes:
//   LaunchedEffect(listingId, selectedPeriod) {
//       isChartLoading = true
//       try {
//           val response = RetrofitClient.api.getListingHistory(listingId, selectedPeriod)
//           if (response.isSuccessful) {
//               history = response.body() ?: emptyList()
//           }
//       } catch (e: Exception) {
//           // Silently fail for chart, keep existing data
//       }
//       isChartLoading = false
//   }
//
// ──────────────────────────────────────────────────────────────────────────────
// NAVIGATION:
// ──────────────────────────────────────────────────────────────────────────────
//
//   Parameters:
//     listingId: Long            — from nav args
//     onBack: () -> Unit         — pop back stack
//     onLogout: () -> Unit       — navigate to login
//     onOrderClick: (Long, String) -> Unit  — (listingId, direction) -> CreateOrderScreen
//
// ──────────────────────────────────────────────────────────────────────────────
// CHART DRAWING GUIDE (Canvas):
// ──────────────────────────────────────────────────────────────────────────────
//
//   Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
//       if (history.isEmpty()) return@Canvas
//
//       val prices = history.map { it.price.toFloat() }
//       val minPrice = prices.min()
//       val maxPrice = prices.max()
//       val priceRange = (maxPrice - minPrice).coerceAtLeast(0.01f)
//
//       val stepX = size.width / (prices.size - 1).coerceAtLeast(1)
//       val padding = 8f
//
//       val path = Path()
//       val fillPath = Path()
//
//       prices.forEachIndexed { i, price ->
//           val x = i * stepX
//           val y = padding + (1f - (price - minPrice) / priceRange) * (size.height - 2 * padding)
//           if (i == 0) {
//               path.moveTo(x, y)
//               fillPath.moveTo(x, y)
//           } else {
//               path.lineTo(x, y)
//               fillPath.lineTo(x, y)
//           }
//       }
//
//       // Close fill path
//       fillPath.lineTo(size.width, size.height)
//       fillPath.lineTo(0f, size.height)
//       fillPath.close()
//
//       val lineColor = if (prices.last() >= prices.first()) SuccessGreen else ErrorRed
//
//       // Draw gradient fill
//       drawPath(
//           path = fillPath,
//           brush = Brush.verticalGradient(
//               colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent)
//           )
//       )
//
//       // Draw line
//       drawPath(
//           path = path,
//           color = lineColor,
//           style = Stroke(width = 2.dp.toPx())
//       )
//   }
//
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDetailScreen(
    listingId: Long,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onOrderClick: (Long, String) -> Unit  // (listingId, direction)
) {
    // TODO: Implement the full screen following the layout described above.
    //       Use OtpScreen.kt as the reference for:
    //       - Scrollable Column layout
    //       - Background orb animation
    //       - SnackbarHost for errors
    //       - Card styling (DarkCard, rounded corners)
    //       - Gradient buttons

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Security Detail #$listingId - TODO",
            color = Color.White,
            fontSize = 20.sp
        )
    }
}
