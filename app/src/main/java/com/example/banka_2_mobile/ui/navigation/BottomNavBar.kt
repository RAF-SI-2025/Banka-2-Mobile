package com.example.banka_2_mobile.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.banka_2_mobile.ui.theme.Indigo400
import com.example.banka_2_mobile.ui.theme.Indigo500
import com.example.banka_2_mobile.ui.theme.TextMuted
import com.example.banka_2_mobile.ui.theme.Violet600

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector? = null,
    val isCenter: Boolean = false
)

// -- Glassmorphism palette --------------------------------------------------

private val GlassBg = Color(0xFF0B1033)
private val GlassBorder = Color(0xFF1A2055)
private val PillActiveBg = Color(0xFF1A1F4D)
private val FabGlow = Indigo500.copy(alpha = 0.35f)

@Composable
fun BottomNavBar(
    items: List<BottomNavItem>,
    currentRoute: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Outer wrapper -- provides space for the floating FAB that rises above the pill
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        // ── Glassmorphism floating pill ─────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color.Black.copy(alpha = 0.45f),
                    spotColor = Indigo500.copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(GlassBg.copy(alpha = 0.92f))
                // subtle top border glow
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                GlassBorder.copy(alpha = 0.6f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = 4.dp.toPx()
                        ),
                        cornerRadius = CornerRadius(28.dp.toPx()),
                        size = Size(size.width, 1.5.dp.toPx())
                    )
                }
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route

                    if (item.isCenter) {
                        // Reserve the space for the center FAB placeholder so
                        // regular items space evenly around it
                        Box(
                            modifier = Modifier.width(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Actual FAB is drawn outside this row (see below)
                        }
                    } else {
                        RegularNavItem(
                            item = item,
                            isSelected = isSelected,
                            onClick = { onItemClick(item.route) }
                        )
                    }
                }
            }
        }

        // ── Center FAB (drawn on top, floating above the pill) ─────────────
        items.find { it.isCenter }?.let { centerItem ->
            val isSelected = currentRoute == centerItem.route
            CenterNavButton(
                item = centerItem,
                isSelected = isSelected,
                onClick = { onItemClick(centerItem.route) },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Center FAB — gradient circle with glow
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CenterNavButton(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.10f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_scale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.50f else 0.25f,
        animationSpec = tween(durationMillis = 350),
        label = "fab_glow"
    )

    val labelColor by animateColorAsState(
        targetValue = if (isSelected) Indigo400 else TextMuted,
        animationSpec = tween(durationMillis = 250),
        label = "fab_label_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .offset(y = (-18).dp) // float above the pill
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .scale(scale)
                // outer glow
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = FabGlow.copy(alpha = glowAlpha),
                    spotColor = Violet600.copy(alpha = glowAlpha)
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Indigo500, Violet600)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) (item.activeIcon ?: item.icon) else item.icon,
                contentDescription = item.label,
                tint = Color.White,
                modifier = Modifier.size(27.dp)
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = labelColor
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Regular nav item — pill indicator + icon + label
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RegularNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) Indigo400 else TextMuted,
        animationSpec = tween(durationMillis = 250),
        label = "icon_color"
    )

    val labelColor by animateColorAsState(
        targetValue = if (isSelected) Indigo400 else TextMuted,
        animationSpec = tween(durationMillis = 250),
        label = "label_color"
    )

    val pillAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "pill_alpha"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon_scale"
    )

    val pillHeight by animateDpAsState(
        targetValue = if (isSelected) 42.dp else 36.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pill_height"
    )

    Box(
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Active pill indicator
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(pillHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    PillActiveBg.copy(alpha = pillAlpha * 0.85f)
                )
                // subtle indigo glow ring around the pill when active
                .then(
                    if (isSelected) {
                        Modifier.drawBehind {
                            drawRoundRect(
                                color = Indigo500.copy(alpha = 0.20f),
                                cornerRadius = CornerRadius(14.dp.toPx()),
                                size = size,
                                topLeft = Offset.Zero
                            )
                        }
                    } else Modifier
                )
        )

        // Content on top of pill
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (isSelected) (item.activeIcon ?: item.icon) else item.icon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconScale)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = labelColor,
                lineHeight = 12.sp
            )
        }
    }
}
