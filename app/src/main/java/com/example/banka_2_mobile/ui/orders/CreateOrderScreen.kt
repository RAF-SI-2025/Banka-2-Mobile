package com.example.banka_2_mobile.ui.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.model.CreateOrderRequest
import com.example.banka_2_mobile.data.model.Listing
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.DarkCardBorder
import com.example.banka_2_mobile.ui.theme.DarkCardElevated
import com.example.banka_2_mobile.ui.theme.ErrorRed
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import kotlinx.coroutines.launch

// Premium emerald/rose colors for BUY/SELL
private val Emerald600 = Color(0xFF059669)
private val Emerald500 = Color(0xFF10B981)
private val Emerald400 = Color(0xFF34D399)
private val Rose600 = Color(0xFFE11D48)
private val Rose500 = Color(0xFFF43F5E)
private val Rose400 = Color(0xFFFB7185)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    listingId: Long,
    initialDirection: String,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var listing by remember { mutableStateOf<Listing?>(null) }
    var direction by remember { mutableStateOf(initialDirection) }
    var orderType by remember { mutableStateOf("MARKET") }
    var quantity by remember { mutableStateOf("") }
    var limitPrice by remember { mutableStateOf("") }
    var stopPrice by remember { mutableStateOf("") }
    var allOrNone by remember { mutableStateOf(false) }
    var isListingLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val orderTypes = listOf("Market", "Limit", "Stop", "Stop-Limit")
    val orderTypeMap = mapOf(
        "Market" to "MARKET",
        "Limit" to "LIMIT",
        "Stop" to "STOP",
        "Stop-Limit" to "STOP_LIMIT"
    )
    val orderTypeDisplayMap = mapOf(
        "MARKET" to "Market",
        "LIMIT" to "Limit",
        "STOP" to "Stop",
        "STOP_LIMIT" to "Stop-Limit"
    )

    val needsLimit = orderType in listOf("LIMIT", "STOP_LIMIT")
    val needsStop = orderType in listOf("STOP", "STOP_LIMIT")

    // Validation
    val quantityValid = quantity.toIntOrNull()?.let { it > 0 } ?: false
    val limitValid = !needsLimit || (limitPrice.toDoubleOrNull()?.let { it > 0 } ?: false)
    val stopValid = !needsStop || (stopPrice.toDoubleOrNull()?.let { it > 0 } ?: false)
    val isFormValid = quantityValid && limitValid && stopValid

    // Estimated value
    val estimatedValue: Double? = run {
        val qty = quantity.toIntOrNull() ?: return@run null
        val price = when {
            needsLimit -> limitPrice.toDoubleOrNull() ?: listing?.price
            needsStop -> stopPrice.toDoubleOrNull() ?: listing?.price
            else -> listing?.price
        } ?: return@run null
        val contractSize = listing?.contractSize ?: 1
        qty * price * contractSize
    }

    LaunchedEffect(listingId) {
        try {
            val response = RetrofitClient.api.getListingById(listingId)
            if (response.isSuccessful) {
                listing = response.body()
            } else if (response.code() == 401) {
                authRepository.clearTokens()
                onLogout()
                return@LaunchedEffect
            }
        } catch (e: Exception) {
            errorMessage = "Greška pri učitavanju hartije."
        }
        isListingLoading = false
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    val isBuy = direction == "BUY"
    val dirBuyColor by animateColorAsState(
        targetValue = if (isBuy) Emerald500 else Color.Transparent,
        animationSpec = tween(250), label = "buyColor"
    )
    val dirSellColor by animateColorAsState(
        targetValue = if (!isBuy) Rose500 else Color.Transparent,
        animationSpec = tween(250), label = "sellColor"
    )
    val actionGradient = if (isBuy)
        Brush.linearGradient(listOf(Emerald500, Emerald600))
    else
        Brush.linearGradient(listOf(Rose500, Rose600))

    val gradientBorder = Brush.linearGradient(
        colors = listOf(Indigo500, Violet600),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = DarkCard,
        unfocusedContainerColor = DarkCard,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Indigo500,
        unfocusedBorderColor = DarkCardBorder,
        cursorColor = Indigo500,
        focusedPlaceholderColor = TextMuted,
        unfocusedPlaceholderColor = TextMuted
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── 1. TOP BAR ──────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Nazad",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Novi nalog",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            // ── 2. LISTING INFO CARD ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        brush = gradientBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(DarkCard)
                    .padding(20.dp)
            ) {
                if (isListingLoading) {
                    // Shimmer placeholder
                    Column {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkCardBorder)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(160.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkCardBorder.copy(alpha = 0.6f))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkCardBorder.copy(alpha = 0.4f))
                        )
                    }
                } else {
                    listing?.let { l ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = l.ticker,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                    // Type badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        Indigo500.copy(alpha = 0.15f),
                                                        Violet600.copy(alpha = 0.15f)
                                                    )
                                                )
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = Indigo500.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = l.listingType,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Indigo400,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = l.name,
                                    fontSize = 13.sp,
                                    color = TextMuted,
                                    maxLines = 1
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "%,.2f".format(l.price),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                                l.changePercent?.let { pct ->
                                    val isPositive = pct >= 0
                                    Text(
                                        text = "${if (isPositive) "+" else ""}%.2f%%".format(pct),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isPositive) Emerald400 else Rose400
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // ── 3. DIRECTION TOGGLE ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // BUY button
                val buyBorderColor by animateColorAsState(
                    if (isBuy) Emerald500 else DarkCardBorder, tween(250), label = "buyBorder"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = if (isBuy) 0.dp else 1.dp,
                            color = buyBorderColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .background(dirBuyColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { direction = "BUY" },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = null,
                            tint = if (isBuy) Color.White else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Kupovina",
                            color = if (isBuy) Color.White else TextMuted,
                            fontWeight = if (isBuy) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }

                // SELL button
                val sellBorderColor by animateColorAsState(
                    if (!isBuy) Rose500 else DarkCardBorder, tween(250), label = "sellBorder"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = if (!isBuy) 0.dp else 1.dp,
                            color = sellBorderColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .background(dirSellColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { direction = "SELL" },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (!isBuy) Color.White else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Prodaja",
                            color = if (!isBuy) Color.White else TextMuted,
                            fontWeight = if (!isBuy) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // ── 4. ORDER TYPE SELECTOR ──────────────────────────────────────
            Text(
                text = "Tip naloga",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                orderTypes.forEach { label ->
                    val apiValue = orderTypeMap[label] ?: label
                    val isSelected = orderType == apiValue
                    val pillBg by animateColorAsState(
                        if (isSelected) Indigo500 else DarkCard, tween(200), label = "pill_$label"
                    )
                    val pillTextColor by animateColorAsState(
                        if (isSelected) Color.White else TextMuted, tween(200), label = "pillTxt_$label"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(pillBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { orderType = apiValue }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            color = pillTextColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // ── 5. QUANTITY INPUT ───────────────────────────────────────────
            Text(
                text = "Koli\u010dina",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Minus stepper
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val current = quantity.toIntOrNull() ?: 0
                            if (current > 1) quantity = (current - 1).toString()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u2212",
                        fontSize = 22.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Quantity field
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "0",
                            color = TextMuted.copy(alpha = 0.5f),
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(14.dp),
                    isError = quantity.isNotEmpty() && !quantityValid,
                    singleLine = true
                )

                // Plus stepper
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val current = quantity.toIntOrNull() ?: 0
                            quantity = (current + 1).toString()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        fontSize = 22.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (quantity.isNotEmpty() && !quantityValid) {
                Text(
                    text = "Unesite pozitivan ceo broj",
                    fontSize = 12.sp,
                    color = ErrorRed,
                    modifier = Modifier.padding(start = 58.dp, top = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            // ── 6. CONDITIONAL PRICE INPUTS ─────────────────────────────────
            AnimatedVisibility(
                visible = needsLimit,
                enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                exit = shrinkVertically(tween(300)) + fadeOut(tween(200))
            ) {
                Column {
                    Text(
                        text = "Limitirana cena",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = limitPrice,
                        onValueChange = { limitPrice = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("0.00", color = TextMuted.copy(alpha = 0.5f)) },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(14.dp),
                        isError = needsLimit && limitPrice.isNotEmpty() && !limitValid,
                        singleLine = true
                    )
                    if (needsLimit && limitPrice.isNotEmpty() && !limitValid) {
                        Text(
                            text = "Unesite validnu cenu",
                            fontSize = 12.sp,
                            color = ErrorRed,
                            modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            AnimatedVisibility(
                visible = needsStop,
                enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                exit = shrinkVertically(tween(300)) + fadeOut(tween(200))
            ) {
                Column {
                    Text(
                        text = "Stop cena",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = stopPrice,
                        onValueChange = { stopPrice = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("0.00", color = TextMuted.copy(alpha = 0.5f)) },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(14.dp),
                        isError = needsStop && stopPrice.isNotEmpty() && !stopValid,
                        singleLine = true
                    )
                    if (needsStop && stopPrice.isNotEmpty() && !stopValid) {
                        Text(
                            text = "Unesite validnu cenu",
                            fontSize = 12.sp,
                            color = ErrorRed,
                            modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // ── 7. AON TOGGLE ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCard)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sve ili ni\u0161ta (AON)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Nalog se izvr\u0161ava samo ako je mogu\u0107e kupiti/prodati celu koli\u010dinu",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = allOrNone,
                        onCheckedChange = { allOrNone = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Indigo500,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkCardBorder,
                            uncheckedBorderColor = Color.Transparent,
                            checkedBorderColor = Color.Transparent
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // ── 8. ORDER SUMMARY CARD ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        brush = gradientBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(DarkCard)
                    .padding(20.dp)
            ) {
                Column {
                    // Header
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
                            text = "Rezime naloga",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Summary rows
                    PremiumSummaryRow("Hartija", listing?.ticker ?: "--")
                    SummaryDivider()
                    PremiumSummaryRow(
                        "Smer",
                        if (isBuy) "Kupovina" else "Prodaja",
                        valueColor = if (isBuy) Emerald400 else Rose400
                    )
                    SummaryDivider()
                    PremiumSummaryRow("Tip", orderTypeDisplayMap[orderType] ?: orderType)
                    SummaryDivider()
                    PremiumSummaryRow(
                        "Koli\u010dina",
                        if (quantity.isEmpty()) "--" else quantity
                    )
                    SummaryDivider()
                    PremiumSummaryRow(
                        "Proc. vrednost",
                        estimatedValue?.let { "%,.2f RSD".format(it) } ?: "--"
                    )
                    SummaryDivider()
                    PremiumSummaryRow("Provizija", "N/A")

                    // Total row
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkCardElevated)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ukupno",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                            Text(
                                text = estimatedValue?.let { "%,.2f RSD".format(it) } ?: "--",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Indigo400
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))

            // ── 9. SUBMIT BUTTON ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isFormValid && !isSubmitting) actionGradient
                        else Brush.linearGradient(
                            listOf(DarkCardBorder.copy(alpha = 0.6f), DarkCardBorder.copy(alpha = 0.6f))
                        )
                    )
            ) {
                Button(
                    onClick = {
                        if (!isFormValid || isSubmitting) return@Button
                        scope.launch {
                            isSubmitting = true
                            try {
                                val request = CreateOrderRequest(
                                    listingId = listingId,
                                    orderType = orderType,
                                    quantity = quantity.toInt(),
                                    direction = direction,
                                    limitPrice = if (needsLimit) limitPrice.toDoubleOrNull() else null,
                                    stopPrice = if (needsStop) stopPrice.toDoubleOrNull() else null,
                                    allOrNone = allOrNone
                                )
                                val response = RetrofitClient.api.createOrder(request)
                                when {
                                    response.isSuccessful -> {
                                        snackbarHostState.showSnackbar("Nalog uspe\u0161no kreiran")
                                        onBack()
                                    }

                                    response.code() == 401 -> {
                                        authRepository.clearTokens()
                                        onLogout()
                                    }

                                    else -> errorMessage =
                                        "Gre\u0161ka pri kreiranju naloga (${response.code()})"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Gre\u0161ka u mre\u017ei. Poku\u0161ajte ponovo."
                            }
                            isSubmitting = false
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    enabled = isFormValid && !isSubmitting,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isBuy) Icons.Filled.KeyboardArrowUp
                                else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (isFormValid) Color.White else TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBuy) "Potvrdi kupovinu" else "Potvrdi prodaju",
                                color = if (isFormValid) Color.White else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // ── 10. BOTTOM SPACING ──────────────────────────────────────────
            Spacer(modifier = Modifier.height(40.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PremiumSummaryRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextMuted,
            fontWeight = FontWeight.Normal
        )
        // Dotted spacer area (simulated with spaced dots)
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SummaryDivider() {
    // Dotted line effect using a very thin dashed-style divider
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(40) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(1.dp)
                    .background(DarkCardBorder.copy(alpha = 0.6f))
            )
        }
    }
}
