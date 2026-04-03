package com.example.banka_2_mobile.ui.otp

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.model.OtpResponse
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.theme.Amber500
import com.example.banka_2_mobile.ui.theme.DarkBg
import com.example.banka_2_mobile.ui.theme.DarkCard
import com.example.banka_2_mobile.ui.theme.DarkCardBorder
import com.example.banka_2_mobile.ui.theme.ErrorRed
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.SuccessGreen
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600
import com.example.banka_2_mobile.ui.theme.WarningYellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var otpData by remember { mutableStateOf<OtpResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val email = authRepository.getEmail() ?: ""

    // Function to fetch OTP
    suspend fun fetchOtp() {
        try {
            val response = RetrofitClient.api.getActiveOtp()
            if (response.isSuccessful) {
                otpData = response.body()
                otpData?.expiresInSeconds?.let { countdown = it }
            } else if (response.code() == 401) {
                // Token expired and refresh failed
                authRepository.clearTokens()
                onLogout()
                return
            } else {
                otpData = OtpResponse(active = false, message = "Greška pri učitavanju")
            }
        } catch (e: Exception) {
            if (otpData == null) {
                otpData = OtpResponse(active = false, message = "Greška u mreži")
            }
            errorMessage = "Greška u mreži. Pokušavam ponovo..."
        }
        isLoading = false
        isRefreshing = false
    }

    // Show error snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    // Polling every 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            fetchOtp()
            delay(5000)
        }
    }

    // Countdown timer (decrements locally every second)
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // Waiting indicator pulse
    val waitingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waiting"
    )

    // Background orb animation
    val orbAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb"
    )

    // Glow pulse for digit boxes
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Shield scale animation for waiting state
    val shieldScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shieldScale"
    )

    // Pulse dot animation for status badge
    val pulseDotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseDot"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch { fetchOtp() }
            },
            modifier = Modifier.padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg)
            ) {
                // Background orbs
                Box(
                    modifier = Modifier
                        .size(350.dp)
                        .alpha(orbAlpha)
                        .align(Alignment.TopStart)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Indigo500.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                radius = 500f
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(350.dp)
                        .alpha(orbAlpha)
                        .align(Alignment.BottomEnd)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Violet600.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                radius = 500f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(48.dp))

                    // Header: Shield icon in gradient circle + title
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Indigo500, Violet600)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = "Shield",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Verifikacija",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "BANKA 2025 \u2022 TIM 2",
                        fontSize = 11.sp,
                        color = TextMuted.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    if (isLoading) {
                        // Loading state
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 60.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .alpha(waitingAlpha)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Indigo500, Violet600)
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Učitavanje...",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    } else if (otpData?.active == true) {
                        // Active OTP display
                        ActiveOtpContent(
                            code = otpData?.code ?: "------",
                            countdown = countdown,
                            attempts = otpData?.attempts ?: 0,
                            maxAttempts = otpData?.maxAttempts ?: 3,
                            glowAlpha = glowAlpha,
                            pulseDotAlpha = pulseDotAlpha
                        )
                    } else {
                        // No active OTP - waiting state
                        WaitingForOtpContent(
                            shieldScale = shieldScale,
                            waitingAlpha = waitingAlpha
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun ActiveOtpContent(
    code: String,
    countdown: Int,
    attempts: Int,
    maxAttempts: Int,
    glowAlpha: Float,
    pulseDotAlpha: Float
) {
    val minutes = countdown / 60
    val seconds = countdown % 60
    val timeString = "%02d:%02d".format(minutes, seconds)

    // Timer color based on remaining time
    val timerColor = when {
        countdown > 60 -> SuccessGreen
        countdown > 30 -> Amber500
        else -> ErrorRed
    }

    // Max seconds for progress calculation (assume 5 minutes = 300s max)
    val maxSeconds = 300
    val progress = countdown.toFloat() / maxSeconds.toFloat()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status badge: "Aktivan" green badge with pulse dot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SuccessGreen.copy(alpha = 0.12f))
                .border(
                    width = 1.dp,
                    color = SuccessGreen.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(pulseDotAlpha)
                    .clip(CircleShape)
                    .background(SuccessGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Aktivan",
                color = SuccessGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 6 individual digit boxes in a row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val digits = code.take(6).padEnd(6, '-')
            digits.forEach { digit ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .drawBehind {
                            // Pulsing glow behind each box
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Indigo500.copy(alpha = glowAlpha),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = size.maxDimension * 0.8f
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                            )
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                        .border(
                            width = 1.dp,
                            color = Indigo500.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Indigo400
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Instruction text below digits
        Text(
            text = "Unesite ovaj kod na web aplikaciji",
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Countdown timer: circular progress indicator around the timer text
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            // Background track
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(120.dp),
                color = DarkCardBorder.copy(alpha = 0.4f),
                strokeWidth = 6.dp,
                trackColor = Color.Transparent,
                strokeCap = StrokeCap.Round
            )
            // Active progress
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.size(120.dp),
                color = timerColor,
                strokeWidth = 6.dp,
                trackColor = Color.Transparent,
                strokeCap = StrokeCap.Round
            )
            // Timer text inside
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timeString,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = timerColor
                )
                Text(
                    text = "preostalo",
                    fontSize = 10.sp,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Attempts indicator: "Pokusaj X od Y" with dot indicators
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .border(
                    width = 1.dp,
                    color = DarkCardBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = "Pokušaj $attempts od $maxAttempts",
                fontSize = 13.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(maxAttempts) { index ->
                    val dotColor = when {
                        index < attempts -> if (attempts >= maxAttempts - 1) WarningYellow else Indigo400
                        else -> DarkCardBorder
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun WaitingForOtpContent(
    shieldScale: Float,
    waitingAlpha: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 24.dp)
    ) {
        // Animated pulsing shield icon
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(shieldScale),
            contentAlignment = Alignment.Center
        ) {
            // Outer glow ring
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .alpha(waitingAlpha * 0.4f)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Indigo500.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
            // Inner circle with shield
            Box(
                modifier = Modifier
                    .size(64.dp)
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
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Indigo500.copy(alpha = 0.4f),
                                Violet600.copy(alpha = 0.4f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = "Shield",
                    tint = Indigo400.copy(alpha = waitingAlpha),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Čekanje na verifikacioni zahtev...",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Verifikacioni kod će se automatski pojaviti",
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 3-step instruction cards
        val steps = listOf(
            "Pokrenite transakciju na web aplikaciji",
            "Verifikacioni kod će se pojaviti ovde",
            "Unesite kod na web aplikaciji"
        )

        steps.forEachIndexed { index, step ->
            InstructionCard(
                stepNumber = index + 1,
                text = step
            )
            if (index < steps.lastIndex) {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun InstructionCard(
    stepNumber: Int,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(
                width = 1.dp,
                color = DarkCardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        // Number badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Indigo500, Violet600)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.85f),
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun extractNameFromEmail(email: String): String {
    val localPart = email.substringBefore("@")
    val parts = localPart.split(".")
    return parts.joinToString(" ") { part ->
        part.replaceFirstChar { it.uppercaseChar() }
    }
}

