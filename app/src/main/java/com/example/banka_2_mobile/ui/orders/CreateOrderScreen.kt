package com.example.banka_2_mobile.ui.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.model.CreateOrderRequest
import com.example.banka_2_mobile.data.model.Listing
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.DarkCardBorder
import com.example.banka_2_mobile.ui.theme.ErrorRed
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.SuccessGreen
import com.example.banka_2_mobile.ui.theme.ErrorRed
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import com.example.banka_2_mobile.ui.theme.WarningYellow
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════════════
// TODO: CreateOrderScreen — Order creation form for BUY/SELL
// ══════════════════════════════════════════════════════════════════════════════
//
// OVERVIEW:
//   Form screen for creating a new stock/futures order. Receives listing ID
//   and initial direction (BUY or SELL) via navigation arguments.
//
// ──────────────────────────────────────────────────────────────────────────────
// UI LAYOUT (scrollable Column):
// ──────────────────────────────────────────────────────────────────────────────
//
//   1. TOP BAR
//      - Back arrow IconButton (Icons.AutoMirrored.Filled.ArrowBack)
//        tint = TextMuted, onClick = onBack()
//      - Title: "Novi nalog" (18.sp, SemiBold, White)
//
//   2. LISTING INFO CARD (fetched from API)
//      - DarkCard box, RoundedCornerShape(16.dp), padding 16.dp
//      - Shows: Ticker (Bold, Monospace) + Name (TextMuted)
//      - Current price (18.sp, Bold, Monospace, White)
//      - Type badge (STOCK/FUTURES pill)
//      - Loading: show shimmer placeholder until listing is fetched
//      - Fetch: RetrofitClient.api.getListingById(listingId)
//
//   3. DIRECTION TOGGLE
//      - Row with two toggle buttons:
//        * KUPI (BUY):
//          - Selected: bg = SuccessGreen, text = White, Bold
//          - Unselected: bg = DarkCard, text = TextMuted
//        * PRODAJ (SELL):
//          - Selected: bg = ErrorRed, text = White, Bold
//          - Unselected: bg = DarkCard, text = TextMuted
//      - Both: RoundedCornerShape(12.dp), equal width (weight 1f), height 48.dp
//      - Gap: 12.dp between buttons
//      - Initialize from nav arg direction
//
//   4. ORDER TYPE SELECTOR
//      - Label: "Tip naloga" (14.sp, TextMuted)
//      - Row of 4 selectable pills:
//        * "Market" | "Limit" | "Stop" | "Stop-Limit"
//        * Selected: bg = Indigo500.copy(alpha=0.2f), border = Indigo500, text = Indigo400
//        * Unselected: bg = DarkCard, border = DarkCardBorder, text = TextMuted
//        * RoundedCornerShape(8.dp), horizontal padding 12.dp, vertical 8.dp
//      - Default: "MARKET"
//      - Map display names to API values: "Market"->"MARKET", "Limit"->"LIMIT",
//        "Stop"->"STOP", "Stop-Limit"->"STOP_LIMIT"
//
//   5. QUANTITY INPUT
//      - Label: "Kolicina" (14.sp, TextMuted)
//      - OutlinedTextField:
//        * keyboardType = KeyboardType.Number
//        * Placeholder: "Unesite kolicinu"
//        * Colors: same as SecuritiesScreen search bar
//          (containerColor=DarkCard, text=White, border=DarkCardBorder, focus=Indigo500)
//        * Validation: must be > 0, show error text in ErrorRed if invalid
//
//   6. CONDITIONAL PRICE INPUTS (based on order type)
//      - LIMIT order → show "Limitirana cena" input:
//        * OutlinedTextField, keyboardType = Decimal
//        * Placeholder: "Unesite limitiranu cenu"
//      - STOP order → show "Stop cena" input:
//        * OutlinedTextField, keyboardType = Decimal
//        * Placeholder: "Unesite stop cenu"
//      - STOP_LIMIT → show BOTH "Limitirana cena" AND "Stop cena" inputs
//      - MARKET → no additional price inputs
//      - Use AnimatedVisibility for smooth show/hide transitions
//
//   7. ALL-OR-NONE CHECKBOX
//      - Row with Checkbox + label text:
//        * "Sve ili nista (AON)" (14.sp, White)
//        * Subtitle: "Nalog se izvrsava samo ako je moguce kupiti/prodati celu kolicinu"
//          (12.sp, TextMuted)
//      - Checkbox tint: Indigo500 when checked
//
//   8. ORDER SUMMARY CARD
//      - DarkCard box, RoundedCornerShape(12.dp)
//      - Title: "Rezime naloga" with gradient accent bar
//      - Rows (label: value):
//        * "Hartija:" listing.ticker
//        * "Smer:" "Kupovina" or "Prodaja" (colored green/red)
//        * "Tip:" orderType display name
//        * "Kolicina:" quantity
//        * "Procenjena vrednost:" quantity * price (for MARKET) or quantity * limitPrice
//          - For FUTURES: multiply by contractSize too
//          - Format as currency with 2 decimal places
//        * "Procenjena provizija:" estimate (can be "N/A" until backend confirms)
//      - Label: 13.sp, TextMuted
//      - Value: 14.sp, White, FontFamily.Monospace
//      - Divider line (DarkCardBorder, 1.dp) between rows
//
//   9. SUBMIT BUTTON
//      - Full width, RoundedCornerShape(12.dp), height 52.dp
//      - BUY direction:
//        * bg = Brush.linearGradient(SuccessGreen, Color(0xFF16A34A))
//        * Text: "Potvrdi kupovinu"
//      - SELL direction:
//        * bg = Brush.linearGradient(ErrorRed, Color(0xFFDC2626))
//        * Text: "Potvrdi prodaju"
//      - Disabled state: reduced alpha, non-clickable
//        * Disabled when: quantity empty/invalid, or limitPrice/stopPrice required but empty
//      - Loading state: show CircularProgressIndicator instead of text
//      - On click:
//        * Build CreateOrderRequest from form state
//        * Call RetrofitClient.api.createOrder(request)
//        * On success: show Snackbar "Nalog uspesno kreiran" + onBack()
//        * On error: show Snackbar with error message
//        * On 401: onLogout()
//
//  10. BOTTOM SPACING
//      - Spacer(height = 40.dp) for safe area
//
// ──────────────────────────────────────────────────────────────────────────────
// STATE VARIABLES:
// ──────────────────────────────────────────────────────────────────────────────
//
//   var listing by remember { mutableStateOf<Listing?>(null) }
//   var direction by remember { mutableStateOf(initialDirection) } // "BUY" or "SELL"
//   var orderType by remember { mutableStateOf("MARKET") }
//   var quantity by remember { mutableStateOf("") }
//   var limitPrice by remember { mutableStateOf("") }
//   var stopPrice by remember { mutableStateOf("") }
//   var allOrNone by remember { mutableStateOf(false) }
//   var isListingLoading by remember { mutableStateOf(true) }
//   var isSubmitting by remember { mutableStateOf(false) }
//   var errorMessage by remember { mutableStateOf<String?>(null) }
//
// ──────────────────────────────────────────────────────────────────────────────
// VALIDATION:
// ──────────────────────────────────────────────────────────────────────────────
//
//   - quantity must be a positive integer (toIntOrNull() != null && > 0)
//   - If orderType is LIMIT or STOP_LIMIT: limitPrice must be valid double > 0
//   - If orderType is STOP or STOP_LIMIT: stopPrice must be valid double > 0
//   - Show inline error text below invalid fields (12.sp, ErrorRed)
//   - Submit button is enabled only when all required fields are valid
//
// ──────────────────────────────────────────────────────────────────────────────
// API CALL (submit):
// ──────────────────────────────────────────────────────────────────────────────
//
//   scope.launch {
//       isSubmitting = true
//       try {
//           val request = CreateOrderRequest(
//               listingId = listingId,
//               orderType = orderType,
//               quantity = quantity.toInt(),
//               direction = direction,
//               limitPrice = if (orderType in listOf("LIMIT", "STOP_LIMIT"))
//                   limitPrice.toDoubleOrNull() else null,
//               stopPrice = if (orderType in listOf("STOP", "STOP_LIMIT"))
//                   stopPrice.toDoubleOrNull() else null,
//               allOrNone = allOrNone
//           )
//           val response = RetrofitClient.api.createOrder(request)
//           if (response.isSuccessful) {
//               snackbarHostState.showSnackbar("Nalog uspesno kreiran")
//               onBack()
//           } else if (response.code() == 401) {
//               authRepository.clearTokens()
//               onLogout()
//           } else {
//               errorMessage = "Greska pri kreiranju naloga (${response.code()})"
//           }
//       } catch (e: Exception) {
//           errorMessage = "Greska u mrezi. Pokusajte ponovo."
//       }
//       isSubmitting = false
//   }
//
// ──────────────────────────────────────────────────────────────────────────────
// NAVIGATION:
// ──────────────────────────────────────────────────────────────────────────────
//
//   Parameters:
//     listingId: Long           — from nav args
//     initialDirection: String  — "BUY" or "SELL" from nav args
//     onBack: () -> Unit        — pop back stack
//     onLogout: () -> Unit      — navigate to login
//
// ══════════════════════════════════════════════════════════════════════════════

/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    listingId: Long,
    initialDirection: String,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    // TODO: Implement the full screen following the layout described above.
    //       Use OtpScreen.kt as reference for scrollable Column layout.
    //       Use HomeScreen.kt AccountCard for card styling.
    //       TextField styling should match web app's dark input style:
    //       - containerColor = DarkCard
    //       - text color = White
    //       - border = DarkCardBorder (unfocused), Indigo500 (focused)
    //       - placeholder = TextMuted

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Create Order (listing=$listingId, dir=$initialDirection) - TODO",
            color = Color.White,
            fontSize = 16.sp
        )
    }
}
*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    listingId: Long,
    initialDirection: String,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    // TODO: Implement the full screen following the layout described above.
    //       Use OtpScreen.kt as reference for scrollable Column layout.
    //       Use HomeScreen.kt AccountCard for card styling.
    //       TextField styling should match web app's dark input style:
    //       - containerColor = DarkCard
    //       - text color = White
    //       - border = DarkCardBorder (unfocused), Indigo500 (focused)
    //       - placeholder = TextMuted

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
    val actionColor = if (isBuy) SuccessGreen else ErrorRed
    val actionGradient = if (isBuy)
        Brush.linearGradient(listOf(SuccessGreen, Color(0xFF16A34A)))
    else
        Brush.linearGradient(listOf(ErrorRed, Color(0xFFDC2626)))

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
            Spacer(modifier = Modifier.height(12.dp))

            // ── 1. TOP BAR ───────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Nazad",
                        tint = TextMuted
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Novi nalog",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // ── 2. LISTING INFO CARD ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(16.dp)
            ) {
                if (isListingLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCardBorder)
                    )
                } else {
                    listing?.let { l ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = l.ticker,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                                Text(text = l.name, fontSize = 13.sp, color = TextMuted)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkCardBorder)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(text = l.listingType, fontSize = 10.sp, color = TextMuted)
                                }
                            }
                            Text(
                                text = "%.2f".format(l.price),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // ── 3. DIRECTION TOGGLE ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("BUY" to "KUPI", "SELL" to "PRODAJ").forEach { (value, label) ->
                    val isSelected = direction == value
                    val selectedColor = if (value == "BUY") SuccessGreen else ErrorRed
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) selectedColor else DarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { direction = value },
                            modifier = Modifier.fillMaxSize(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else TextMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // ── 4. ORDER TYPE SELECTOR ───────────────────────────────────────
            Text(text = "Tip naloga", fontSize = 14.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                orderTypes.forEach { label ->
                    val apiValue = orderTypeMap[label] ?: label
                    val isSelected = orderType == apiValue
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Indigo500.copy(alpha = 0.2f) else DarkCard
                            )
                            .then(
                                if (isSelected) Modifier else Modifier
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Button(
                            onClick = { orderType = apiValue },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.wrapContentSize()
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                color = if (isSelected) Indigo400 else TextMuted,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // ── 5. QUANTITY INPUT ────────────────────────────────────────────
            Text(text = "Količina", fontSize = 14.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Unesite količinu", color = TextMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp),
                isError = quantity.isNotEmpty() && !quantityValid,
                singleLine = true
            )
            if (quantity.isNotEmpty() && !quantityValid) {
                Text(
                    text = "Unesite pozitivan ceo broj",
                    fontSize = 12.sp,
                    color = ErrorRed,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // ── 6. CONDITIONAL PRICE INPUTS ──────────────────────────────────
            AnimatedVisibility(
                visible = needsLimit,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Text(text = "Limitirana cena", fontSize = 14.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = limitPrice,
                        onValueChange = { limitPrice = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Unesite limitiranu cenu", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        isError = needsLimit && limitPrice.isNotEmpty() && !limitValid,
                        singleLine = true
                    )
                    if (needsLimit && limitPrice.isNotEmpty() && !limitValid) {
                        Text(
                            text = "Unesite validnu cenu",
                            fontSize = 12.sp,
                            color = ErrorRed,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            AnimatedVisibility(
                visible = needsStop,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Text(text = "Stop cena", fontSize = 14.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = stopPrice,
                        onValueChange = { stopPrice = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Unesite stop cenu", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        isError = needsStop && stopPrice.isNotEmpty() && !stopValid,
                        singleLine = true
                    )
                    if (needsStop && stopPrice.isNotEmpty() && !stopValid) {
                        Text(
                            text = "Unesite validnu cenu",
                            fontSize = 12.sp,
                            color = ErrorRed,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ── 7. AON CHECKBOX ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = allOrNone,
                    onCheckedChange = { allOrNone = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Indigo500,
                        uncheckedColor = DarkCardBorder
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = "Sve ili ništa (AON)", fontSize = 14.sp, color = Color.White)
                    Text(
                        text = "Nalog se izvršava samo ako je moguće kupiti/prodati celu količinu",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // ── 8. ORDER SUMMARY CARD ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .padding(16.dp)
            ) {
                Column {
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Rezime naloga",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    SummaryRow("Hartija:", listing?.ticker ?: "-")
                    HorizontalDivider(
                        color = DarkCardBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    SummaryRow(
                        "Smer:",
                        if (isBuy) "Kupovina" else "Prodaja",
                        valueColor = actionColor
                    )
                    HorizontalDivider(
                        color = DarkCardBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    SummaryRow("Tip:", orderTypeDisplayMap[orderType] ?: orderType)
                    HorizontalDivider(
                        color = DarkCardBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    SummaryRow("Količina:", if (quantity.isEmpty()) "-" else quantity)
                    HorizontalDivider(
                        color = DarkCardBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    SummaryRow(
                        "Proc. vrednost:",
                        estimatedValue?.let { "%,.2f RSD".format(it) } ?: "-"
                    )
                    HorizontalDivider(
                        color = DarkCardBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    SummaryRow("Proc. provizija:", "N/A")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // ── 9. SUBMIT BUTTON ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isFormValid && !isSubmitting) actionGradient else Brush.linearGradient(
                            listOf(DarkCardBorder, DarkCardBorder)
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
                                        snackbarHostState.showSnackbar("Nalog uspešno kreiran")
                                        onBack()
                                    }

                                    response.code() == 401 -> {
                                        authRepository.clearTokens()
                                        onLogout()
                                    }

                                    else -> errorMessage =
                                        "Greška pri kreiranju naloga (${response.code()})"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Greška u mreži. Pokušajte ponovo."
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
                        Text(
                            text = if (isBuy) "Potvrdi kupovinu" else "Potvrdi prodaju",
                            color = if (isFormValid) Color.White else TextMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // ── 10. BOTTOM SPACING ───────────────────────────────────────────
            Spacer(modifier = Modifier.height(40.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = TextMuted)
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
