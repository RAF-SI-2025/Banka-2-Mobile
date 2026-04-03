package com.example.banka_2_mobile.ui.exchange

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.model.ExchangeRate
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.DarkCardBorder
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var rates by remember { mutableStateOf<List<ExchangeRate>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Calculator state
    var amount by remember { mutableStateOf("") }
    var fromCurrency by remember { mutableStateOf("EUR") }
    var toCurrency by remember { mutableStateOf("RSD") }
    var convertedResult by remember { mutableStateOf<String?>(null) }
    var isCalculating by remember { mutableStateOf(false) }

    suspend fun fetchRates() {
        try {
            val response = RetrofitClient.api.getExchangeRates()
            if (response.isSuccessful) {
                rates = response.body() ?: emptyList()
            } else if (response.code() == 401) {
                authRepository.clearTokens()
                onLogout()
                return
            } else {
                errorMessage = "Greska pri ucitavanju kursne liste (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Greska u mrezi. Proverite konekciju."
        }
        isLoading = false
        isRefreshing = false
    }

    suspend fun calculate() {
        val amountVal = amount.replace(",", ".").toDoubleOrNull()
        if (amountVal == null || amountVal <= 0) {
            errorMessage = "Unesite ispravan iznos"
            return
        }
        isCalculating = true
        try {
            val response = RetrofitClient.api.calculateExchange(
                amount = amountVal,
                fromCurrency = fromCurrency,
                toCurrency = toCurrency
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val formatter = NumberFormat.getNumberInstance(Locale("sr", "RS"))
                    formatter.minimumFractionDigits = 2
                    formatter.maximumFractionDigits = 2
                    convertedResult = "${formatter.format(body.convertedAmount)} $toCurrency"
                }
            } else {
                errorMessage = "Greska pri konverziji (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Greska u mrezi"
        }
        isCalculating = false
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    LaunchedEffect(Unit) {
        fetchRates()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val orbAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.2f,
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
                scope.launch { fetchRates() }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg)
            ) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .alpha(orbAlpha)
                        .align(Alignment.TopEnd)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Violet600.copy(alpha = 0.3f), Color.Transparent),
                                radius = 400f
                            )
                        )
                )

                if (isLoading) {
                    LoadingShimmer()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Indigo500, Violet600)
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    imageVector = Icons.Filled.Language,
                                    contentDescription = null,
                                    tint = Indigo400,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Kursna lista",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Kursevi valuta prema RSD",
                                fontSize = 13.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(start = 52.dp)
                            )
                        }

                        if (rates.isEmpty()) {
                            item { EmptyRatesState() }
                        } else {
                            items(rates) { rate ->
                                RateCard(rate)
                            }
                        }

                        // Calculator section
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            CalculatorSection(
                                amount = amount,
                                onAmountChange = { amount = it },
                                fromCurrency = fromCurrency,
                                toCurrency = toCurrency,
                                onFromChange = { fromCurrency = it },
                                onToChange = { toCurrency = it },
                                result = convertedResult,
                                isCalculating = isCalculating,
                                currencies = listOf("RSD") + rates.map { it.currency },
                                onCalculate = {
                                    scope.launch { calculate() }
                                }
                            )
                        }

                        // Bottom spacing for navbar
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

@Composable
private fun RateCard(rate: ExchangeRate) {
    val gradient = currencyBadgeGradient(rate.currency)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(
                width = 1.dp,
                color = DarkCardBorder,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row: Currency badge + code
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Currency circle badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(brush = Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rate.currency.take(3),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = rate.currency,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = currencyFullName(rate.currency),
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DarkCardBorder.copy(alpha = 0.6f))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Rate values row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Buy rate
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Kupovni",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatRate(rate.buyRate),
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Middle rate (highlighted)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Srednji",
                        fontSize = 11.sp,
                        color = Indigo400,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Indigo500.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = formatRate(rate.middleRate ?: rate.rate),
                            fontSize = 15.sp,
                            color = Indigo400,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Sell rate
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Prodajni",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatRate(rate.sellRate),
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculatorSection(
    amount: String,
    onAmountChange: (String) -> Unit,
    fromCurrency: String,
    toCurrency: String,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    result: String?,
    isCalculating: Boolean,
    currencies: List<String>,
    onCalculate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Indigo500.copy(alpha = 0.5f),
                        Violet600.copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .background(DarkCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with gradient accent
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
                    imageVector = Icons.Filled.CurrencyExchange,
                    contentDescription = null,
                    tint = Indigo400,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kalkulator",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Amount input - large monospace
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("Iznos") },
                placeholder = {
                    Text(
                        "0.00",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Indigo500,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedLabelColor = Indigo400,
                    unfocusedLabelColor = TextMuted,
                    cursorColor = Indigo500,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = TextMuted,
                    unfocusedPlaceholderColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Currency selectors with swap icon between them
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencySelector(
                    label = "Iz valute",
                    selected = fromCurrency,
                    currencies = currencies,
                    onSelect = onFromChange,
                    modifier = Modifier.weight(1f)
                )

                // Swap icon
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
                        )
                        .border(
                            width = 1.dp,
                            color = DarkCardBorder,
                            shape = CircleShape
                        )
                        .clickable {
                            val temp = fromCurrency
                            onFromChange(toCurrency)
                            onToChange(temp)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapVert,
                        contentDescription = "Zameni valute",
                        tint = Indigo400,
                        modifier = Modifier.size(20.dp)
                    )
                }

                CurrencySelector(
                    label = "U valutu",
                    selected = toCurrency,
                    currencies = currencies,
                    onSelect = onToChange,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Calculate button - gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Indigo500, Violet600)
                        )
                    )
                    .clickable { onCalculate() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCalculating) "Racunam..." else "Izracunaj",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }

            // Result
            if (result != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Indigo500.copy(alpha = 0.08f),
                                    Violet600.copy(alpha = 0.12f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Indigo500.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Rezultat konverzije",
                            fontSize = 12.sp,
                            color = TextMuted,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Indigo400,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencySelector(
    label: String,
    selected: String,
    currencies: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkBg)
                .border(
                    width = 1.dp,
                    color = DarkCardBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Small currency circle badge
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                currencyBadgeGradient(selected)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selected.take(2),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selected,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkCard)
        ) {
            currencies.distinct().forEach { currency ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            currencyBadgeGradient(currency)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currency.take(2),
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = currency,
                                color = if (currency == selected) Indigo400 else Color.White,
                                fontWeight = if (currency == selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    onClick = {
                        onSelect(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyRatesState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Indigo500.copy(alpha = 0.15f),
                            Violet600.copy(alpha = 0.15f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = DarkCardBorder,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CurrencyExchange,
                contentDescription = null,
                tint = Indigo400,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Nema kursne liste",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Kursna lista trenutno nije dostupna",
            fontSize = 14.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header shimmer
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .alpha(shimmerAlpha)
                    .background(DarkCardBorder)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .alpha(shimmerAlpha)
                    .background(DarkCard)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .alpha(shimmerAlpha)
                    .background(DarkCard)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .padding(start = 52.dp)
                .width(160.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .alpha(shimmerAlpha)
                .background(DarkCard)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Rate card shimmers
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .alpha(shimmerAlpha)
                    .background(DarkCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DarkCardBorder)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkCardBorder)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkCardBorder)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DarkCardBorder.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(3) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DarkCardBorder)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DarkCardBorder)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Calculator shimmer
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .alpha(shimmerAlpha)
                .background(DarkCard)
        )
    }
}

// ─── Helpers ───────────────────────────────────────────────

private fun formatRate(rate: Double?): String {
    if (rate == null) return "-"
    val formatter = NumberFormat.getNumberInstance(Locale("sr", "RS"))
    formatter.minimumFractionDigits = 4
    formatter.maximumFractionDigits = 4
    return formatter.format(rate)
}

private fun currencyBadgeGradient(currency: String): List<Color> {
    return when (currency.uppercase()) {
        "EUR" -> listOf(Indigo500, Violet600)
        "USD" -> listOf(Color(0xFF22C55E), Color(0xFF16A34A))
        "CHF" -> listOf(Color(0xFFEF4444), Color(0xFFDC2626))
        "GBP" -> listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))
        "JPY" -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
        "CAD" -> listOf(Color(0xFFEC4899), Color(0xFFDB2777))
        "AUD" -> listOf(Color(0xFF14B8A6), Color(0xFF0D9488))
        "SEK", "NOK", "DKK" -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
        "RSD" -> listOf(Color(0xFF6366F1), Color(0xFF4338CA))
        else -> listOf(Color(0xFF6B7280), Color(0xFF4B5563))
    }
}

private fun currencyFullName(currency: String): String {
    return when (currency.uppercase()) {
        "EUR" -> "Evro"
        "USD" -> "Americki dolar"
        "CHF" -> "Svajcarski franak"
        "GBP" -> "Britanska funta"
        "JPY" -> "Japanski jen"
        "CAD" -> "Kanadski dolar"
        "AUD" -> "Australijski dolar"
        "SEK" -> "Svedska kruna"
        "NOK" -> "Norveska kruna"
        "DKK" -> "Danska kruna"
        "RSD" -> "Srpski dinar"
        "HUF" -> "Madjarska forinta"
        "CZK" -> "Ceska kruna"
        "PLN" -> "Poljski zlot"
        "BAM" -> "Konvertibilna marka"
        "HRK" -> "Hrvatska kuna"
        "TRY" -> "Turska lira"
        "RUB" -> "Ruska rublja"
        "CNY" -> "Kineski juan"
        else -> currency
    }
}
