package com.example.banka_2_mobile.ui.securities

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.example.banka_2_mobile.ui.theme.DarkCardElevated
import com.example.banka_2_mobile.ui.theme.ErrorRed
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.SuccessGreen
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════════════
// SecuritiesScreen — Premium Fintech Berza Listing
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritiesScreen(
    onLogout: () -> Unit,
    onListingClick: (Long) -> Unit
) {
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
                errorMessage = "Greska pri ucitavanju berze (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Greska u mrezi. Proverite konekciju."
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

    // Derived market overview stats
    val totalListings = listings.size
    val bestPerformer = listings.maxByOrNull { it.changePercent ?: Double.MIN_VALUE }
    val totalVolume = listings.sumOf { it.volume ?: 0L }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
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
                // ══════════════════════════════════════════════════════════════
                // 1. HEADER — "Berza" with TrendingUp icon + gradient bar
                // ══════════════════════════════════════════════════════════════
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Gradient accent bar
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Indigo500, Violet600),
                                        start = Offset(0f, 0f),
                                        end = Offset(0f, Float.POSITIVE_INFINITY)
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        // Icon with gradient background circle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Indigo500.copy(alpha = 0.2f),
                                            Violet600.copy(alpha = 0.2f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = Indigo400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Berza",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Trgovanje hartijama od vrednosti",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ══════════════════════════════════════════════════════════════
                // 2. PILL TAB SELECTOR with animated slide indicator
                // ══════════════════════════════════════════════════════════════
                item {
                    PillTabSelector(
                        tabs = tabs,
                        activeTab = activeTab,
                        onTabSelected = { index ->
                            activeTab = index
                            searchQuery = ""
                            currentPage = 0
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ══════════════════════════════════════════════════════════════
                // 3. SEARCH BAR — pill shaped, semi-transparent
                // ══════════════════════════════════════════════════════════════
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Pretrazi hartije...",
                                color = TextMuted.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = Indigo400,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkCard.copy(alpha = 0.8f),
                            unfocusedContainerColor = DarkCard.copy(alpha = 0.6f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Indigo500.copy(alpha = 0.6f),
                            unfocusedBorderColor = DarkCardBorder.copy(alpha = 0.5f),
                            cursorColor = Indigo500
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ══════════════════════════════════════════════════════════════
                // 4. MARKET OVERVIEW — 3 mini stat cards
                // ══════════════════════════════════════════════════════════════
                if (!isLoading && listings.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MarketStatCard(
                                label = "Ukupno",
                                value = totalListings.toString(),
                                icon = Icons.Filled.ShowChart,
                                accentColor = Indigo500,
                                modifier = Modifier.weight(1f)
                            )
                            MarketStatCard(
                                label = "Najbolji",
                                value = bestPerformer?.ticker ?: "-",
                                subValue = bestPerformer?.let { formatChangePercent(it.changePercent) },
                                subColor = if ((bestPerformer?.changePercent ?: 0.0) >= 0) SuccessGreen else ErrorRed,
                                icon = Icons.Filled.TrendingUp,
                                accentColor = SuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                            MarketStatCard(
                                label = "Volumen",
                                value = formatVolume(totalVolume),
                                icon = Icons.Filled.ShowChart,
                                accentColor = Violet600,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // ══════════════════════════════════════════════════════════════
                // 5. LOADING — shimmer skeleton cards
                // ══════════════════════════════════════════════════════════════
                if (isLoading) {
                    // Shimmer stat cards row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            repeat(3) {
                                ShimmerBox(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(80.dp),
                                    cornerRadius = 16
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    items(4) {
                        ListingShimmerCard()
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // ══════════════════════════════════════════════════════════════
                // 6. SECURITIES LIST — premium cards
                // ══════════════════════════════════════════════════════════════
                if (!isLoading) {
                    if (listings.isNotEmpty()) {
                        items(listings) { listing ->
                            PremiumListingCard(
                                listing = listing,
                                onClick = { onListingClick(listing.id) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        // ── 7. EMPTY STATE ──────────────────────────────────
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 60.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Magnifying glass icon in gradient circle
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    DarkCard,
                                                    DarkCardElevated
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    DarkCardBorder.copy(alpha = 0.6f),
                                                    DarkCardBorder.copy(alpha = 0.2f)
                                                )
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = TextMuted.copy(alpha = 0.6f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Nema rezultata za pretragu",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Pokusajte sa drugim pojmom pretrage",
                                    fontSize = 13.sp,
                                    color = TextMuted.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // ── BOTTOM SPACING ──────────────────────────────────────────
                item { Spacer(modifier = Modifier.height(90.dp)) }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}


// ══════════════════════════════════════════════════════════════════════════════
// Pill Tab Selector — animated pill-shaped tabs with gradient active indicator
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PillTabSelector(
    tabs: List<String>,
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(DarkCard)
            .border(
                width = 1.dp,
                color = DarkCardBorder.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = activeTab == index
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else TextMuted,
                    animationSpec = tween(200),
                    label = "tabTextColor$index"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .then(
                            if (isSelected) {
                                Modifier.background(
                                    Brush.linearGradient(
                                        colors = listOf(Indigo500, Violet600)
                                    )
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}


// ══════════════════════════════════════════════════════════════════════════════
// Market Stat Card — mini overview card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MarketStatCard(
    label: String,
    value: String,
    subValue: String? = null,
    subColor: Color = TextMuted,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(
                width = 1.dp,
                color = DarkCardBorder.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Column {
            // Icon row
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Label
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))

            // Value
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Sub-value (e.g., change percent for best performer)
            if (subValue != null) {
                Text(
                    text = subValue,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = subColor
                )
            }
        }
    }
}


// ══════════════════════════════════════════════════════════════════════════════
// Premium Listing Card — Bloomberg/Robinhood style security card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PremiumListingCard(listing: Listing, onClick: () -> Unit) {
    val changePercent = listing.changePercent ?: 0.0
    val isPositive = changePercent >= 0
    val changeColor = if (isPositive) SuccessGreen else ErrorRed
    val accentBrush = Brush.verticalGradient(
        colors = if (isPositive) {
            listOf(SuccessGreen, SuccessGreen.copy(alpha = 0.4f))
        } else {
            listOf(ErrorRed, ErrorRed.copy(alpha = 0.4f))
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(
                width = 1.dp,
                color = DarkCardBorder.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left colored accent line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentBrush)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 14.dp)
            ) {
                // Top row: Ticker + Name + Exchange badge (top-right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Left column: Ticker + Name
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = listing.ticker,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            // Listing type badge
                            SmallTypeBadge(
                                text = if (listing.listingType == "STOCK") "AKC" else "FUT",
                                color = Indigo500
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = listing.name,
                            fontSize = 13.sp,
                            color = TextMuted.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Exchange badge top-right
                    listing.exchangeAcronym?.let { exchange ->
                        ExchangeBadge(text = exchange)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom row: Price + Change badge + Mini volume bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Price
                    Text(
                        text = formatPrice(listing.price),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Mini volume bar
                        listing.volume?.let { vol ->
                            VolumeBar(
                                volume = vol,
                                maxVolume = 100_000_000L, // normalized scale
                                color = changeColor.copy(alpha = 0.4f)
                            )
                        }

                        // Change% pill badge
                        ChangeBadge(
                            changePercent = changePercent,
                            isPositive = isPositive,
                            color = changeColor
                        )
                    }
                }
            }
        }
    }
}


// ══════════════════════════════════════════════════════════════════════════════
// Change Badge — green/red pill with arrow icon
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChangeBadge(
    changePercent: Double,
    isPositive: Boolean,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = if (isPositive) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = formatChangePercent(changePercent),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = color
            )
        }
    }
}


// ══════════════════════════════════════════════════════════════════════════════
// Volume Bar — mini bar chart indicator
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun VolumeBar(
    volume: Long,
    maxVolume: Long,
    color: Color
) {
    val fillRatio = (volume.toFloat() / maxVolume.toFloat()).coerceIn(0.05f, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // 4 mini bars of increasing height to simulate volume
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.height(20.dp)
        ) {
            val heights = listOf(0.3f, 0.55f, 0.4f, fillRatio)
            heights.forEach { h ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight(h)
                        .clip(RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp))
                        .background(color)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = formatVolume(volume),
            fontSize = 9.sp,
            color = TextMuted.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace
        )
    }
}


// ══════════════════════════════════════════════════════════════════════════════
// Exchange Badge — small top-right badge
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ExchangeBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Indigo500.copy(alpha = 0.12f),
                        Violet600.copy(alpha = 0.12f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = Indigo500.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = Indigo400,
            letterSpacing = 0.5.sp
        )
    }
}


// ══════════════════════════════════════════════════════════════════════════════
// Small Type Badge — listing type indicator (AKC / FUT)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SmallTypeBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color.copy(alpha = 0.8f),
            letterSpacing = 0.5.sp
        )
    }
}


// ══════════════════════════════════════════════════════════════════════════════
// Shimmer Components
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shimmerAlpha"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(DarkCard.copy(alpha = shimmerAlpha))
    )
}

@Composable
private fun ListingShimmerCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmerCard")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shimmerCardAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = DarkCardBorder.copy(alpha = shimmerAlpha * 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                // Ticker shimmer
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkCardBorder.copy(alpha = shimmerAlpha))
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Name shimmer
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkCardBorder.copy(alpha = shimmerAlpha * 0.7f))
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Price shimmer
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkCardBorder.copy(alpha = shimmerAlpha))
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                // Exchange badge shimmer
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkCardBorder.copy(alpha = shimmerAlpha * 0.5f))
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Change badge shimmer
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkCardBorder.copy(alpha = shimmerAlpha * 0.6f))
                )
            }
        }
    }
}


// ══════════════════════════════════════════════════════════════════════════════
// Helper functions (unchanged logic)
// ══════════════════════════════════════════════════════════════════════════════

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
