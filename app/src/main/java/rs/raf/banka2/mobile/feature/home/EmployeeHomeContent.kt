package rs.raf.banka2.mobile.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rs.raf.banka2.mobile.core.auth.UserProfile
import rs.raf.banka2.mobile.core.auth.UserRole
import rs.raf.banka2.mobile.core.format.MoneyFormatter
import rs.raf.banka2.mobile.core.ui.components.GlassCard
import rs.raf.banka2.mobile.data.dto.order.OrderDto
import rs.raf.banka2.mobile.data.dto.portfolio.PortfolioSummaryDto

@Composable
fun EmployeeHomeContent(
    state: HomeState,
    role: UserRole,
    onNavigate: (HomeAction) -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { EmployeeHero(state.profile, role, onNavigate) }
        item { EmployeeStats(role = role, summary = state.portfolioSummary, employeesActive = state.employeesActive, employeesTotal = state.employeesTotal, recentOrders = state.recentOrders) }
        item { SectionTitle("Brze akcije", icon = Icons.Filled.AccountBalance) }
        item { EmployeeQuickActions(role, onNavigate) }
        item { SectionTitle("Poslednji nalozi", icon = Icons.Filled.ShoppingCart) }
        if (state.recentOrders.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "Nema nedavnih naloga.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(state.recentOrders, key = { it.id }) { order ->
                OrderRow(order)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun EmployeeHero(profile: UserProfile?, role: UserRole, onNavigate: (HomeAction) -> Unit) {
    val panelLabel = when {
        role.isAdmin -> "Admin panel"
        role.isSupervisor -> "Supervizor panel"
        role == UserRole.Agent -> "Agent panel"
        else -> "Employee portal"
    }
    val description = when {
        role.isAdmin -> "Upravljajte zaposlenima, klijentima, kreditima i pratite rad banke."
        role.isSupervisor -> "Nadgledajte naloge, upravljajte aktuarima i pratite trgovinu."
        else -> "Pratite naloge i trgovinu na berzi."
    }
    val greeting = when (java.time.LocalTime.now().hour) {
        in 0..5 -> "Dobra noc"
        in 6..11 -> "Dobro jutro"
        in 12..17 -> "Dobar dan"
        else -> "Dobro vece"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF2E1065))))
            .padding(24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF34D399))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    panelLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFA5B4FC),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                "$greeting, ${profile?.firstName ?: profile?.fullName ?: "Korisnice"}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC7D2FE)
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroPill("Berza", Icons.AutoMirrored.Filled.ShowChart) { onNavigate(HomeAction.OpenSecurities) }
                HeroPill("Portfolio", Icons.Filled.WorkOutline) { onNavigate(HomeAction.OpenPortfolio) }
                HeroPill("Nalozi", Icons.Filled.Receipt) { onNavigate(HomeAction.OpenSupervisorOrders) }
            }
        }
    }
}

@Composable
private fun HeroPill(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable
private fun EmployeeStats(
    role: UserRole,
    summary: PortfolioSummaryDto?,
    employeesActive: Int,
    employeesTotal: Int,
    recentOrders: List<OrderDto>
) {
    val portfolioValue = summary?.totalValue ?: 0.0
    val portfolioProfit = summary?.totalProfit ?: 0.0
    val taxOwed = summary?.taxOwed ?: 0.0
    val pending = recentOrders.count { it.status == "PENDING" }
    val approved = recentOrders.count { it.status == "APPROVED" }
    val done = recentOrders.count { it.status == "DONE" }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "Portfolio",
                value = MoneyFormatter.format(portfolioValue, 0) + " USD",
                icon = Icons.Filled.WorkOutline,
                accent = Color(0xFF6366F1),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = if (portfolioProfit >= 0) "Profit" else "Gubitak",
                value = (if (portfolioProfit >= 0) "+" else "") + MoneyFormatter.format(portfolioProfit, 0) + " USD",
                icon = if (portfolioProfit >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                accent = if (portfolioProfit >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (role.isAdmin) {
                StatCard(
                    title = "Zaposleni",
                    value = "$employeesActive / $employeesTotal",
                    icon = Icons.Filled.People,
                    accent = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            } else {
                StatCard(
                    title = "Porez (RSD)",
                    value = MoneyFormatter.format(taxOwed, 0),
                    icon = Icons.Filled.AccountBalance,
                    accent = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }
            StatCard(
                title = "Nalozi",
                value = "P:$pending · A:$approved · D:$done",
                icon = Icons.Filled.Receipt,
                accent = Color(0xFFF43F5E),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
    }
}

private data class EmpQuickItem(val label: String, val sub: String, val icon: ImageVector, val action: HomeAction, val gradient: List<Color>)

@Composable
private fun EmployeeQuickActions(role: UserRole, onNavigate: (HomeAction) -> Unit) {
    val items = buildList {
        if (role.isAdmin) {
            add(EmpQuickItem("Zaposleni", "Upravljanje", Icons.Filled.People, HomeAction.OpenEmployees, listOf(Color(0xFF6366F1), Color(0xFF7C3AED))))
            add(EmpQuickItem("Novi zaposleni", "Kreiranje naloga", Icons.Filled.PersonAdd, HomeAction.OpenEmployees, listOf(Color(0xFF3B82F6), Color(0xFF6366F1))))
        }
        add(EmpQuickItem("Racuni", "Klijentski racuni", Icons.Filled.AccountBalanceWallet, HomeAction.OpenEmployeeAccounts, listOf(Color(0xFF10B981), Color(0xFF047857))))
        add(EmpQuickItem("Klijenti", "Pregled i izmena", Icons.Filled.People, HomeAction.OpenEmployeeClients, listOf(Color(0xFFF59E0B), Color(0xFFEA580C))))
        add(EmpQuickItem("Kartice", "Zahtevi", Icons.Filled.CreditCard, HomeAction.OpenEmployeeCardRequests, listOf(Color(0xFF14B8A6), Color(0xFF0891B2))))
        if (role.isAdmin || role.isSupervisor) {
            add(EmpQuickItem("Orderi", "Odobravanje", Icons.AutoMirrored.Filled.ShowChart, HomeAction.OpenSupervisorOrders, listOf(Color(0xFFF43F5E), Color(0xFFDB2777))))
            add(EmpQuickItem("Aktuari", "Limiti", Icons.AutoMirrored.Filled.TrendingUp, HomeAction.OpenActuaries, listOf(Color(0xFFA855F7), Color(0xFF7C3AED))))
            add(EmpQuickItem("Porez", "Portal", Icons.Filled.Calculate, HomeAction.OpenTax, listOf(Color(0xFFF59E0B), Color(0xFFB45309))))
            add(EmpQuickItem("Profit Banke", "Aktuari + fondovi", Icons.Filled.AccountBalance, HomeAction.OpenProfitBank, listOf(Color(0xFF06B6D4), Color(0xFF0E7490))))
            add(EmpQuickItem("Berze", "Test mode", Icons.AutoMirrored.Filled.ShowChart, HomeAction.OpenExchangesManagement, listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))))
            add(EmpQuickItem("Marzni +", "Novi margin", Icons.Filled.Add, HomeAction.OpenMarginCreate, listOf(Color(0xFFE11D48), Color(0xFFBE123C))))
        }
        add(EmpQuickItem("Berza", "Hartije", Icons.AutoMirrored.Filled.ShowChart, HomeAction.OpenSecurities, listOf(Color(0xFF14B8A6), Color(0xFF0891B2))))
        add(EmpQuickItem("Portfolio", "Moje hartije", Icons.Filled.WorkOutline, HomeAction.OpenPortfolio, listOf(Color(0xFFF97316), Color(0xFFEA580C))))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { item ->
                    EmpQuickTile(item, modifier = Modifier.weight(1f), onClick = { onNavigate(item.action) })
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmpQuickTile(item: EmpQuickItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(item.gradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(item.sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OrderRow(order: OrderDto) {
    val isBuy = order.direction.equals("BUY", true)
    val color = if (isBuy) Color(0xFF10B981) else Color(0xFFEF4444)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBuy) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = (if (isBuy) "Kupovina " else "Prodaja ") + (order.listingTicker ?: "?"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${order.quantity} kom · ${order.orderType}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = when (order.status) {
                        "PENDING" -> "Ceka"
                        "APPROVED" -> "Aktivan"
                        "DONE" -> "Zavrsen"
                        "DECLINED" -> "Odbijen"
                        else -> order.status
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

